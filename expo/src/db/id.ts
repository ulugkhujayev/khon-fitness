import * as Crypto from 'expo-crypto';

/** Eight hex characters from a random UUID, the same shape as newId() in the Kotlin app. */
export const newId = (): string => Crypto.randomUUID().replace(/-/g, '').slice(0, 8);
