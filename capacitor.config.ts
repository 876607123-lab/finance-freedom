import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.financefreedom.app',
  appName: '我的财务自由',
  webDir: 'www',
  android: {
    allowMixedContent: false
  }
};

export default config;
