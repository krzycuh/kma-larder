/** @type {import('tailwindcss').Config} */

// Theme-aware palette: every color family the app uses reads its shades from CSS variables
// (src/theme.css) – warm "pantry" values by default, `html.dark` swaps in the "cellar" theme.
const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950];
const themed = (family) =>
  Object.fromEntries(SHADES.map((shade) => [shade, `rgb(var(--c-${family}-${shade}) / <alpha-value>)`]));

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        white: 'rgb(var(--c-white) / <alpha-value>)',
        gray: themed('gray'),
        blue: themed('blue'),
        red: themed('red'),
        green: themed('green'),
        amber: themed('amber'),
        yellow: themed('yellow'),
        star: 'rgb(var(--c-star) / <alpha-value>)',
        // Statistics charts: one hue per chart (src/theme.css)
        chart: {
          1: 'rgb(var(--c-chart-1) / <alpha-value>)',
          2: 'rgb(var(--c-chart-2) / <alpha-value>)',
          3: 'rgb(var(--c-chart-3) / <alpha-value>)',
          4: 'rgb(var(--c-chart-4) / <alpha-value>)',
          5: 'rgb(var(--c-chart-5) / <alpha-value>)',
          muted: 'rgb(var(--c-chart-muted) / <alpha-value>)',
        },
        // Camera scanner: always black and white, whatever the theme
        scanner: {
          bg: '#000000',
          fg: '#ffffff',
          muted: '#e5e7eb',
          ink: '#111827',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      },
    },
  },
  plugins: [],
}
