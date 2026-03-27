import { Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import View from './pages/View';
import A11y from './pages/A11y';
import NotFoundPage from './pages/NotFound';

const AppRoutes: React.FC = () => {
    return <Routes>
        <Route index element={<Home />} />
        <Route path="view/:csv" element={<View />} />
        <Route path="view.xhtml" element={<View />} />
        <Route path="index.xhtml" element={<View />} />
        <Route path="hash/:csv" element={<View />} />
        <Route path="a11y" element={<A11y />} />
        {/* */}
        <Route path="*" element={<NotFoundPage />} />
    </Routes>;
}

export default AppRoutes;