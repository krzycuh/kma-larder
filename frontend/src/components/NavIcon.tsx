export type NavIconName = 'stock' | 'expiring' | 'replenish' | 'profile' | 'users' | 'camera' | 'jar' | 'members';

// Outline icons drawn on a 24px grid with a 1.75px stroke
const paths: Record<NavIconName, string[]> = {
  // shelves with jars
  stock: ['M3 9.5h18', 'M3 16.5h18', 'M3 4v17', 'M21 4v17', 'M6.5 9.5V7a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2.5', 'M13.5 16.5V14a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2.5'],
  // calendar with a clock hand
  expiring: ['M4.5 5.5h15a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1Z', 'M3.5 9.5h17', 'M8 3v4', 'M16 3v4', 'M12 12.5v3l2 1.5'],
  // shopping basket
  replenish: ['M3.5 9.5h17l-1.6 9a1.5 1.5 0 0 1-1.5 1.25H6.6a1.5 1.5 0 0 1-1.5-1.25z', 'M8 9.5 11 4', 'M16 9.5 13 4', 'M9.5 13v3.5', 'M14.5 13v3.5'],
  profile: ['M15.75 7.5a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z', 'M4.5 20.25a7.5 7.5 0 0 1 15 0'],
  users: [
    'M12 8.25a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    'M3 19.5a6 6 0 0 1 12 0',
    'M15.75 5.4a2.75 2.75 0 1 1 0 5.2',
    'M17.25 13.6a5.25 5.25 0 0 1 3.75 5.15',
  ],
  members: [
    'M12 8.25a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    'M3 19.5a6 6 0 0 1 12 0',
    'M15.75 5.4a2.75 2.75 0 1 1 0 5.2',
    'M17.25 13.6a5.25 5.25 0 0 1 3.75 5.15',
  ],
  camera: [
    'M9.6 4.75h4.8a1.5 1.5 0 0 1 1.25.67l1.1 1.66a.75.75 0 0 0 .62.34h2.38a1.5 1.5 0 0 1 1.5 1.5v8.83a1.5 1.5 0 0 1-1.5 1.5H4.25a1.5 1.5 0 0 1-1.5-1.5V8.92a1.5 1.5 0 0 1 1.5-1.5h2.38a.75.75 0 0 0 .62-.34l1.1-1.66a1.5 1.5 0 0 1 1.25-.67Z',
    'M15.25 12.75a3.25 3.25 0 1 1-6.5 0 3.25 3.25 0 0 1 6.5 0Z',
  ],
  // a preserving jar
  jar: ['M8 3.5h8', 'M8 3.5v2.5a1 1 0 0 1-.4.8L7 7.5v11a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-11l-.6-.7a1 1 0 0 1-.4-.8V3.5', 'M7 11.5h10'],
};

/**
 * Line icon for a main navigation entry or an action button (camera = scan a barcode).
 * Strokes in the current text color; the dark theme adds a warm glow (theme.css).
 */
export function NavIcon({ name, className = 'h-5 w-5' }: { name: NavIconName; className?: string }) {
  return (
    <svg
      className={`nav-icon flex-shrink-0 ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
