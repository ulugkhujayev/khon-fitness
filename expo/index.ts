/**
 * App entry: Expo Router, plus the Android widget task handler, which has to be registered here so the
 * launcher can draw widgets while the app is not running.
 */
import 'expo-router/entry';
import { registerWidgets } from '@/widgets/register';

registerWidgets();
