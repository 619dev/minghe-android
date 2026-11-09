import { registerPlugin } from '@capacitor/core';

export interface Account {
  host: string;
  port: number;
  extension: string;
  mode: 'sdes' | 'zrtp';
  md5: boolean;
  certificate?: string;
  savePassword?: boolean;
  hasSavedPassword?: boolean;
  language: string;
}
export interface Status {
  account?: Account;
  registrationCode?: number;
  callCode?: number;
  history?: { peer: string; time: number; incoming: boolean; answered: boolean }[];
  registration?: 'None' | 'Progress' | 'Ok' | 'Refreshing' | 'Failed' | 'Cleared';
  call?: 'idle' | 'incoming' | 'ringing' | 'securing' | 'calling' | 'active' | 'failed';
  peer?: string;
  sas?: string;
  verified?: boolean;
}
export const sip = registerPlugin<{
  setAppearance(options: { dark: boolean }): Promise<void>;
  status(): Promise<Status>;
  discoverCertificate(options: { host: string; port: number }): Promise<{ certificate?: string; fingerprint?: string }>;
  connect(options: { account: Account & { password: string } }): Promise<void>;
  disconnect(): Promise<void>;
  batterySettings(): Promise<void>;
  dial(options: { peer: string }): Promise<void>;
  answer(): Promise<void>;
  hangup(): Promise<void>;
  verify(): Promise<void>;
}>('Sip');
