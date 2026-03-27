import React from 'react';
import { useAppContext } from '../AppContext';
import { useAnswerRequiredDialogButtons } from '../AppButtons';
import { BaseApp as ReactBaseApp, BaseAppProps } from '../BaseApp';
import { useOptionalResourceApiContext } from '../ResourceApiContext';
import { useMessageDialog } from './Dialog';
import { useTemporalMessage } from './TemporalMessage';
import AppBar from './AppBar';
import Menu, { MenuEntry } from './Menu';
import OfflineMessage from './OfflineMessage';
import { useToolbarMenuIcon } from './ToolbarMenuIcon';

export type MuiBaseAppProps = BaseAppProps & {
    title: string;
    titleLogo?: string;
    titleLogoStyle?: any;
    version?: string;
    logo?: string;
    logoStyle?: any;
    menuTitle?: string;
    menuEntries?: MenuEntry[];
    menuOnTitleClose?: () => void;
    menuShrinkDisabled?: boolean;
    additionalHeaderComponents?: React.ReactElement | React.ReactElement[];
    appbarStyle?: any;
    appbarBackgroundColor?: string;
    appbarBackgroundImg?: string;
};

const MuiComponentsConfigurer: React.FC = () => {
    const [messageDialogShow, messageDialogComponent] = useMessageDialog();
    const [temporalMessageShow, temporalMessageComponent] = useTemporalMessage();
    const resourceApiContext = useOptionalResourceApiContext();
    const {
        setMessageDialogShow,
        setTemporalMessageShow,
    } = useAppContext();
    const getAnswerRequiredButtons = useAnswerRequiredDialogButtons();
    const openAnswerRequiredDialog = (
        title: string | undefined,
        question: string,
        trueFalseAnswerRequired: boolean,
        availableAnswers?: string[]) => {
        return messageDialogShow(
            title ?? 'Atenció',
            question,
            getAnswerRequiredButtons(trueFalseAnswerRequired, availableAnswers));
    }
    React.useEffect(() => {
        setMessageDialogShow(messageDialogShow);
        setTemporalMessageShow(temporalMessageShow);
        if (resourceApiContext != null) {
            resourceApiContext.setOpenAnswerRequiredDialog(openAnswerRequiredDialog);
        }
    }, [])
    return <>
        {messageDialogComponent}
        {temporalMessageComponent}
    </>;
}

export const MuiBaseApp: React.FC<MuiBaseAppProps> = (props) => {
    const {
        title,
        titleLogo,
        titleLogoStyle,
        version,
        logo,
        logoStyle,
        menuTitle,
        menuEntries,
        menuOnTitleClose,
        menuShrinkDisabled,
        appbarStyle,
        appbarBackgroundColor,
        appbarBackgroundImg,
        formFieldComponents,
        additionalHeaderComponents,
        contentComponentProps,
        children,
        ...otherProps
    } = props;
    const mergedFormFieldComponents = [...(formFieldComponents ?? [])];
    const { shrink, buttonComponent: menuButton } = useToolbarMenuIcon();
    const appbarComponent = <AppBar
        title={title}
        titleLogo={titleLogo}
        titleLogoStyle={titleLogoStyle}
        version={version}
        logo={logo}
        logoStyle={logoStyle}
        menuButton={!menuShrinkDisabled && menuEntries != null ? menuButton : undefined}
        additionalComponents={additionalHeaderComponents}
        style={appbarStyle}
        backgroundColor={appbarBackgroundColor}
        backgroundImg={appbarBackgroundImg} />;
    const menuComponent = menuEntries != null ? <Menu title={menuTitle} entries={menuEntries} onTitleClose={menuOnTitleClose} shrink={shrink} /> : null;
    const offlineComponent = <OfflineMessage />;
    return <ReactBaseApp
        formFieldComponents={mergedFormFieldComponents}
        {...otherProps}
        contentComponentProps={{
            appbarComponent,
            menuComponent,
            offlineComponent,
            ...contentComponentProps,
        }}>
        <>
            <MuiComponentsConfigurer />
            {children}
        </>
    </ReactBaseApp>;
}

export default MuiBaseApp;