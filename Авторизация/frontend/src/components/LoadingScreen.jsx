export function LoadingScreen({ label = 'Загружаем ThermoSelect…' }) {
  return (
    <div className="loading-screen" role="status">
      <span className="spinner" />
      <span>{label}</span>
    </div>
  );
}
