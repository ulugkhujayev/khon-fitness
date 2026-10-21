import { noResults } from '@/domain/editors';
import { ContentEmpty } from './ContentEmpty';

/**
 * What a list under a search shows when nothing matches (iOS ContentUnavailableView.search): a magnifying
 * glass, "No Results for “row”" and what to try. Used in screens and in pick sheets alike.
 */
export function SearchEmpty({ query }: { query: string }) {
  const { title, hint } = noResults(query);
  return <ContentEmpty icon={{ ios: 'magnifyingglass', android: 'search', web: 'search' }} title={title} text={hint} />;
}
