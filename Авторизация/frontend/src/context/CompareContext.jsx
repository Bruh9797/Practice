import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const CompareContext = createContext(null);
const STORAGE_KEY = 'thermoselect.compare';

function initialIds() {
  try {
    const value = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(value) ? value.slice(0, 4).map(Number).filter(Number.isFinite) : [];
  } catch {
    return [];
  }
}

export function CompareProvider({ children }) {
  const [ids, setIdsState] = useState(initialIds);

  const setIds = useCallback((next) => {
    setIdsState((current) => {
      const value = typeof next === 'function' ? next(current) : next;
      const unique = [...new Set(value.map(Number).filter(Number.isFinite))].slice(0, 4);
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(unique));
      return unique;
    });
  }, []);

  const toggle = useCallback((id) => {
    const numericId = Number(id);
    if (ids.includes(numericId)) {
      setIds(ids.filter((value) => value !== numericId));
      return 'removed';
    }
    if (ids.length >= 4) return 'limit';
    setIds([...ids, numericId]);
    return 'added';
  }, [ids, setIds]);

  const value = useMemo(() => ({ ids, toggle, clear: () => setIds([]), has: (id) => ids.includes(Number(id)) }), [ids, toggle, setIds]);
  return <CompareContext.Provider value={value}>{children}</CompareContext.Provider>;
}

export function useCompare() {
  const value = useContext(CompareContext);
  if (!value) throw new Error('useCompare должен использоваться внутри CompareProvider');
  return value;
}
