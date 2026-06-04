import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './AppRoutes';
import './styles/global.css';

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}
