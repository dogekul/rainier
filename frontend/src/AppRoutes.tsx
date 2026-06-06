import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import DemandsPage from './pages/Demand';
import DemandRequirementsPage from './pages/DemandRequirement';
import Home from './pages/Home';
import Login from './pages/Login';
import OrganizationsPage from './pages/Organization';
import RequirementsPage from './pages/Requirement';
import UsersPage from './pages/User';
import UserOrganizationsPage from './pages/UserOrganization';

/** App route tree. Exported separately from {@link App} so tests can wrap with MemoryRouter. */
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/org" element={<Navigate to="/org/organizations" replace />} />
          <Route path="/org/organizations" element={<OrganizationsPage />} />
          <Route path="/org/users" element={<UsersPage />} />
          <Route path="/org/user-organizations" element={<UserOrganizationsPage />} />
          <Route path="/pm" element={<Navigate to="/pm/demands" replace />} />
          <Route path="/pm/demands" element={<DemandsPage />} />
          <Route path="/pm/requirements" element={<RequirementsPage />} />
          <Route path="/pm/demand-requirements" element={<DemandRequirementsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
