import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { CompareProvider, useCompare } from './CompareContext.jsx';

function Harness() {
  const compare = useCompare();
  const [message, setMessage] = useState('');
  return <><output>{compare.ids.join(',')}</output>{[1,2,3,4,5].map((id) => <button key={id} onClick={() => setMessage(compare.toggle(id))}>{id}</button>)}<span>{message}</span></>;
}

describe('CompareContext', () => {
  beforeEach(() => sessionStorage.clear());

  it('хранит максимум четыре уникальные модели', () => {
    render(<CompareProvider><Harness /></CompareProvider>);
    [1,2,3,4].forEach((id) => fireEvent.click(screen.getByRole('button', { name: String(id) })));
    expect(screen.getByRole('status')).toHaveTextContent('1,2,3,4');
    fireEvent.click(screen.getByRole('button', { name: '5' }));
    expect(screen.getByText('limit')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('1,2,3,4');
  });

  it('повторный выбор удаляет модель', () => {
    render(<CompareProvider><Harness /></CompareProvider>);
    fireEvent.click(screen.getByRole('button', { name: '1' }));
    fireEvent.click(screen.getByRole('button', { name: '1' }));
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });
});
