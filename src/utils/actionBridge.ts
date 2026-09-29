/**
 * Android App Action Bridge for MYRA AI Assistant
 * Provides unified, platform-aware execution for:
 * - openWhatsApp()
 * - openApp(appName)
 * - openUrl(url)
 * - makeCall(phoneNumber)
 * - callContact(contactName)
 *
 * When packaged as an Android APK with WebView, communicates with window.AndroidBridge.
 * When running in a web browser, uses validated deep links and tel: schemes.
 */

export interface ContactInfo {
  name: string;
  phone: string;
  relationship?: string;
}

export interface CallContactResult {
  status: 'called' | 'clarification_needed' | 'not_found' | 'error';
  contact?: ContactInfo;
  matches?: ContactInfo[];
  message: string;
  phoneNumber?: string;
}

export interface ActionBridgeResult {
  success: boolean;
  action: string;
  platform: 'android-native' | 'browser-deeplink';
  message: string;
}

// Global interface for Android WebView bridge
declare global {
  interface Window {
    AndroidBridge?: {
      openApp?: (appName: string) => boolean | string;
      makeCall?: (phoneNumber: string) => boolean | string;
      callContact?: (contactName: string) => boolean | string;
      openWhatsApp?: () => boolean | string;
      openUrl?: (url: string) => boolean | string;
    };
    Android?: {
      openApp?: (appName: string) => boolean | string;
      makeCall?: (phoneNumber: string) => boolean | string;
      callContact?: (contactName: string) => boolean | string;
      openWhatsApp?: () => boolean | string;
      openUrl?: (url: string) => boolean | string;
    };
    MyraNativeBridge?: {
      openApp?: (appName: string) => boolean | string;
      makeCall?: (phoneNumber: string) => boolean | string;
      callContact?: (contactName: string) => boolean | string;
      openWhatsApp?: () => boolean | string;
      openUrl?: (url: string) => boolean | string;
    };
  }
}

class ActionBridgeService {
  private defaultContacts: ContactInfo[] = [
    { name: 'Mummy', relationship: 'Mom', phone: '+919876543210' },
    { name: 'Mom', relationship: 'Mother', phone: '+919876543210' },
    { name: 'Mother', relationship: 'Mom', phone: '+919876543210' },
    { name: 'Maa', relationship: 'Mom', phone: '+919876543210' },
    { name: 'Papa', relationship: 'Dad', phone: '+919876543211' },
    { name: 'Dad', relationship: 'Father', phone: '+919876543211' },
    { name: 'Father', relationship: 'Dad', phone: '+919876543211' },
    { name: 'Priya', relationship: 'Prime Contact', phone: '+919876543213' },
    { name: 'Rahul Office', relationship: 'Colleague', phone: '+919876543214' },
    { name: 'Rahul Sharma', relationship: 'Friend', phone: '+919876543215' },
    { name: 'Bhai', relationship: 'Brother', phone: '+919876543216' },
    { name: 'Sister', relationship: 'Behan', phone: '+919876543217' },
  ];

  /**
   * Check if native Android bridge is available (when inside Android APK WebView)
   */
  public hasNativeBridge(): boolean {
    if (typeof window === 'undefined') return false;
    return Boolean(
      window.AndroidBridge || window.Android || window.MyraNativeBridge
    );
  }

  private getNativeBridge() {
    if (typeof window === 'undefined') return null;
    return window.AndroidBridge || window.Android || window.MyraNativeBridge || null;
  }

  /**
   * Get all registered contacts (from storage + default)
   */
  public getContacts(primeContact?: { name: string; phone: string }): ContactInfo[] {
    const list = [...this.defaultContacts];
    if (primeContact && primeContact.name && primeContact.phone) {
      const idx = list.findIndex(
        (c) => c.name.toLowerCase() === primeContact.name.toLowerCase()
      );
      if (idx >= 0) {
        list[idx].phone = primeContact.phone;
      } else {
        list.unshift({
          name: primeContact.name,
          phone: primeContact.phone,
          relationship: 'Prime Contact',
        });
      }
    }

    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem('myra_custom_contacts');
        if (stored) {
          const parsed: ContactInfo[] = JSON.parse(stored);
          if (Array.isArray(parsed)) {
            list.push(...parsed);
          }
        }
      } catch (e) {
        console.warn('Failed to load custom contacts:', e);
      }
    }
    return list;
  }

  /**
   * Save a new contact
   */
  public saveContact(contact: ContactInfo) {
    if (typeof window === 'undefined') return;
    try {
      const existing = localStorage.getItem('myra_custom_contacts');
      const list: ContactInfo[] = existing ? JSON.parse(existing) : [];
      list.push(contact);
      localStorage.setItem('myra_custom_contacts', JSON.stringify(list));
    } catch (e) {
      console.warn('Failed to save contact:', e);
    }
  }

  /**
   * openWhatsApp() - executes native intent or browser deep link
   */
  public openWhatsApp(phone?: string, text?: string): ActionBridgeResult {
    const bridge = this.getNativeBridge();
    if (bridge && typeof bridge.openWhatsApp === 'function') {
      try {
        bridge.openWhatsApp();
        return {
          success: true,
          action: 'openWhatsApp',
          platform: 'android-native',
          message: 'WhatsApp opened via Android Native Bridge',
        };
      } catch (err) {
        console.warn('Native openWhatsApp failed, falling back:', err);
      }
    }

    // Web Browser fallback
    try {
      let targetUrl = 'https://web.whatsapp.com';
      if (phone) {
        const cleanPhone = phone.replace(/[^0-9+]/g, '');
        const encodedText = text ? encodeURIComponent(text) : '';
        targetUrl = `https://wa.me/${cleanPhone}${encodedText ? `?text=${encodedText}` : ''}`;
      }
      
      // On mobile browsers, try opening the whatsapp:// deep link first
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      if (isMobile) {
        window.location.href = phone ? `whatsapp://send?phone=${phone}` : 'whatsapp://';
      } else {
        window.open(targetUrl, '_blank', 'noopener,noreferrer');
      }

      return {
        success: true,
        action: 'openWhatsApp',
        platform: 'browser-deeplink',
        message: 'WhatsApp opened via web deep link',
      };
    } catch (e: any) {
      return {
        success: false,
        action: 'openWhatsApp',
        platform: 'browser-deeplink',
        message: `Failed to open WhatsApp: ${e?.message || 'Unsupported browser environment'}`,
      };
    }
  }

  /**
   * openApp(appName) - executes native intent or web deep link
   */
  public openApp(appName: string): ActionBridgeResult {
    const cleanApp = appName.trim();
    const lower = cleanApp.toLowerCase();
    const bridge = this.getNativeBridge();

    // 1. If Native Android Bridge is present
    if (bridge && typeof bridge.openApp === 'function') {
      try {
        const res = bridge.openApp(cleanApp);
        return {
          success: Boolean(res),
          action: `openApp(${cleanApp})`,
          platform: 'android-native',
          message: `${cleanApp} opened via Android Native Bridge`,
        };
      } catch (err) {
        console.warn('Native openApp failed, falling back:', err);
      }
    }

    // 2. Specific App Deep Links for Web
    if (lower.includes('whatsapp')) {
      return this.openWhatsApp();
    }

    if (lower.includes('youtube')) {
      window.open('https://www.youtube.com', '_blank');
      return {
        success: true,
        action: 'openApp(YouTube)',
        platform: 'browser-deeplink',
        message: 'YouTube opened',
      };
    }

    if (lower.includes('instagram')) {
      window.open('https://www.instagram.com', '_blank');
      return {
        success: true,
        action: 'openApp(Instagram)',
        platform: 'browser-deeplink',
        message: 'Instagram opened',
      };
    }

    if (lower.includes('chrome') || lower.includes('browser')) {
      window.open('https://www.google.com', '_blank');
      return {
        success: true,
        action: 'openApp(Chrome)',
        platform: 'browser-deeplink',
        message: 'Chrome / Browser opened',
      };
    }

    if (lower.includes('spotify') || lower.includes('music')) {
      window.open('https://open.spotify.com', '_blank');
      return {
        success: true,
        action: 'openApp(Spotify)',
        platform: 'browser-deeplink',
        message: 'Spotify opened',
      };
    }

    if (lower.includes('gmail') || lower.includes('mail')) {
      window.open('https://mail.google.com', '_blank');
      return {
        success: true,
        action: 'openApp(Gmail)',
        platform: 'browser-deeplink',
        message: 'Gmail opened',
      };
    }

    if (lower.includes('maps') || lower.includes('map')) {
      window.open('https://maps.google.com', '_blank');
      return {
        success: true,
        action: 'openApp(Maps)',
        platform: 'browser-deeplink',
        message: 'Google Maps opened',
      };
    }

    if (lower.includes('settings')) {
      // Browsers cannot open Android Settings without native bridge
      return {
        success: false,
        action: 'openApp(Settings)',
        platform: 'browser-deeplink',
        message: 'Device Settings requires running inside the MYRA Android APK',
      };
    }

    // Generic search for other apps
    window.open(`https://www.google.com/search?q=${encodeURIComponent(cleanApp)}`, '_blank');
    return {
      success: true,
      action: `openApp(${cleanApp})`,
      platform: 'browser-deeplink',
      message: `Navigated to ${cleanApp}`,
    };
  }

  /**
   * openUrl(url) - open specific web link safely
   */
  public openUrl(url: string): ActionBridgeResult {
    let cleanUrl = url.trim();
    if (!cleanUrl.startsWith('http://') && !cleanUrl.startsWith('https://')) {
      cleanUrl = `https://${cleanUrl}`;
    }

    const bridge = this.getNativeBridge();
    if (bridge && typeof bridge.openUrl === 'function') {
      try {
        bridge.openUrl(cleanUrl);
        return {
          success: true,
          action: `openUrl(${cleanUrl})`,
          platform: 'android-native',
          message: `URL opened via Native Bridge: ${cleanUrl}`,
        };
      } catch (e) {
        console.warn('Native openUrl failed, falling back:', e);
      }
    }

    try {
      window.open(cleanUrl, '_blank', 'noopener,noreferrer');
      return {
        success: true,
        action: `openUrl(${cleanUrl})`,
        platform: 'browser-deeplink',
        message: `Opened ${cleanUrl}`,
      };
    } catch (e: any) {
      return {
        success: false,
        action: `openUrl(${cleanUrl})`,
        platform: 'browser-deeplink',
        message: `Failed to open URL: ${e?.message || 'Popup blocked'}`,
      };
    }
  }

  /**
   * makeCall(phoneNumber) - places direct call or opens phone dialer
   */
  public makeCall(phoneNumber: string): ActionBridgeResult {
    const cleanNumber = phoneNumber.replace(/[^0-9+]/g, '');
    if (!cleanNumber) {
      return {
        success: false,
        action: 'makeCall',
        platform: 'browser-deeplink',
        message: 'Invalid phone number provided',
      };
    }

    const bridge = this.getNativeBridge();
    if (bridge && typeof bridge.makeCall === 'function') {
      try {
        bridge.makeCall(cleanNumber);
        return {
          success: true,
          action: `makeCall(${cleanNumber})`,
          platform: 'android-native',
          message: `Initiating phone call to ${cleanNumber} via Android Bridge`,
        };
      } catch (err) {
        console.warn('Native makeCall failed, falling back to tel link:', err);
      }
    }

    // Browser tel: protocol (opens device native phone dialer)
    try {
      window.location.href = `tel:${cleanNumber}`;
      return {
        success: true,
        action: `makeCall(${cleanNumber})`,
        platform: 'browser-deeplink',
        message: `Phone dialer opened with ${cleanNumber}`,
      };
    } catch (e: any) {
      return {
        success: false,
        action: `makeCall(${cleanNumber})`,
        platform: 'browser-deeplink',
        message: `Unable to open dialer: ${e?.message || 'Protocol not supported'}`,
      };
    }
  }

  /**
   * callContact(contactName) - searches contacts, handles exact match, multiple matches, or none
   */
  public callContact(
    contactName: string,
    primeContact?: { name: string; phone: string }
  ): CallContactResult {
    const cleanQuery = contactName.trim().toLowerCase();
    if (!cleanQuery) {
      return {
        status: 'not_found',
        message: 'Please specify the name of the contact you want to call.',
      };
    }

    const bridge = this.getNativeBridge();
    if (bridge && typeof bridge.callContact === 'function') {
      try {
        const res = bridge.callContact(contactName);
        if (res) {
          return {
            status: 'called',
            message: `Calling ${contactName} via Android Contacts Bridge...`,
          };
        }
      } catch (err) {
        console.warn('Native callContact failed, searching local contacts:', err);
      }
    }

    const contacts = this.getContacts(primeContact);

    // Exact matches
    const exactMatches = contacts.filter(
      (c) =>
        c.name.toLowerCase() === cleanQuery ||
        (c.relationship && c.relationship.toLowerCase() === cleanQuery)
    );

    if (exactMatches.length === 1) {
      const target = exactMatches[0];
      this.makeCall(target.phone);
      return {
        status: 'called',
        contact: target,
        phoneNumber: target.phone,
        message: `Calling ${target.name} (${target.phone})...`,
      };
    }

    // Partial / substring matches
    const partialMatches = contacts.filter(
      (c) =>
        c.name.toLowerCase().includes(cleanQuery) ||
        (c.relationship && c.relationship.toLowerCase().includes(cleanQuery))
    );

    if (partialMatches.length === 1) {
      const target = partialMatches[0];
      this.makeCall(target.phone);
      return {
        status: 'called',
        contact: target,
        phoneNumber: target.phone,
        message: `Calling ${target.name} (${target.phone})...`,
      };
    }

    if (partialMatches.length > 1) {
      const names = partialMatches.map((m) => `${m.name} (${m.phone})`).join(', ');
      return {
        status: 'clarification_needed',
        matches: partialMatches,
        message: `I found ${partialMatches.length} contacts matching "${contactName}": ${names}. Which one should I call?`,
      };
    }

    return {
      status: 'not_found',
      message: `I could not find "${contactName}" in your contacts. Please say the phone number or add them in settings.`,
    };
  }
}

export const ActionBridge = new ActionBridgeService();
