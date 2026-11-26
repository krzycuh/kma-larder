import { describe, expect, it } from 'vitest';
import { fireEvent, render } from '@testing-library/react';
import { ProductThumb } from '../ProductThumb';

describe('ProductThumb', () => {
  it('shows the category pictogram when there is no picture', () => {
    const { container } = render(<ProductThumb product={{ category: 'HONEY', imageUrl: null }} />);
    expect(container.querySelector('img')).toBeNull();
    expect(container.textContent).toBe('🍯');
  });

  it('shows the picture and falls back to the pictogram when it fails to load', () => {
    const { container } = render(<ProductThumb product={{ category: 'CANNED', imageUrl: 'https://example.org/x.jpg' }} />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    expect(img?.getAttribute('src')).toBe('https://example.org/x.jpg');
    fireEvent.error(img!);
    expect(container.querySelector('img')).toBeNull();
    expect(container.textContent).toBe('🥫');
  });

  it('tries again when the link changes', () => {
    const { container, rerender } = render(<ProductThumb product={{ category: 'CANNED', imageUrl: 'https://example.org/a.jpg' }} />);
    fireEvent.error(container.querySelector('img')!);
    rerender(<ProductThumb product={{ category: 'CANNED', imageUrl: 'https://example.org/b.jpg' }} />);
    expect(container.querySelector('img')?.getAttribute('src')).toBe('https://example.org/b.jpg');
  });
});
