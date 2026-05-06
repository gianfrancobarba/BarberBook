import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('App', () => {
  it('renders BarberBook title', () => {
    render(<App />);
    expect(screen.getByText(/BarberBook/i)).toBeInTheDocument();
  });
});
