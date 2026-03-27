import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import BaseCaibApp from './components/BaseCaibApp';
import AppRoutes from './AppRoutes';

//import headerBackground from './assets/header_background2.jpg';

function App() {
    const { t } = useTranslation();
    const [searchParams] = useSearchParams();
    const langSearchParam = searchParams?.get('lang');
    const menuEntries = [{
        id: 'home',
        title: t('menu.home'),
        icon: 'home',
        to: '/',
    }, {
        id: 'a11y',
        title: t('menu.a11y'),
        to: '/a11y',
    }];

    return <BaseCaibApp
        code="concsv"
        title={t('title')}
        headerBackgroundColor="#ffffff"
        //headerBackgroundImage={headerBackground}
        version={import.meta.env.VITE_APP_VERSION}
        defaultLanguage={langSearchParam ?? undefined}
        menuEntries={menuEntries}>
        <AppRoutes />
    </BaseCaibApp>;
}

export default App
