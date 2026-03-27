import React from 'react';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import ListItemButton from '@mui/material/ListItemButton';
import Icon from '@mui/material/Icon';
import Divider from '@mui/material/Divider';
import Box from '@mui/material/Box';
import { styled, useTheme, Theme, CSSObject } from '@mui/material/styles';
import { useAppContext } from '../AppContext';
import { IconButton } from '@mui/material';

const DRAWER_WIDTH = 240;

export type MenuEntry = {
    id: string;
    title?: string;
    to?: string;
    icon?: string;
    children?: MenuEntry[];
    divider?: boolean;
};

export type MenuProps = {
    title?: string;
    entries?: MenuEntry[];
    level?: number;
    onTitleClose?: () => void;
    shrink?: boolean;
};

type ListMenuContentProps = MenuProps & {};

type MenuItemProps = React.PropsWithChildren & {
    primary: string;
    to?: string;
    icon?: string;
    level?: number;
    selected?: boolean;
    shrink?: boolean;
}

type MenuTitleProps = {
    title: string;
    onClose?: () => void;
};

const openedMixin = (theme: Theme): CSSObject => ({
    width: DRAWER_WIDTH,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
    }),
    overflowX: 'hidden',
});

const closedMixin = (theme: Theme): CSSObject => ({
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    overflowX: 'hidden',
    width: `calc(${theme.spacing(6)} + 1px)`,
    [theme.breakpoints.up('sm')]: {
        width: `calc(${theme.spacing(7)} + 1px)`,
    },
});

const StyledDrawer = styled(Drawer, { shouldForwardProp: (prop) => prop !== 'open' })(
    ({ theme, open }) => ({
        width: DRAWER_WIDTH,
        flexShrink: 0,
        whiteSpace: 'nowrap',
        boxSizing: 'border-box',
        ...(open && {
            ...openedMixin(theme),
            '& .MuiDrawer-paper': openedMixin(theme),
        }),
        ...(!open && {
            ...closedMixin(theme),
            '& .MuiDrawer-paper': closedMixin(theme),
        }),
    }),
);

const StyledList = styled(List)<{ component?: React.ElementType }>({
    '& .MuiListItemButton-root': {
        paddingLeft: 24,
        paddingRight: 24,
    },
    '& .MuiListItemIcon-root': {
        minWidth: 0,
        marginRight: 16,
    },
    '& .MuiSvgIcon-root': {
        fontSize: 20,
    },
    paddingTop: 0,
    paddingBottom: 0,
});

const isCurrentMenuEntryOrAnyChildrenSelected = (menuEntry: MenuEntry, locationPath: string): boolean => {
    const selected = menuEntry.to != null && (menuEntry.to === locationPath || locationPath.startsWith(menuEntry.to + '/'));
    return selected || menuEntry.children?.find(e => isCurrentMenuEntryOrAnyChildrenSelected(e, locationPath)) != null;
}

const MenuItem: React.FC<MenuItemProps> = (props) => {
    const {
        primary,
        to,
        icon,
        level = 0,
        selected,
        shrink,
        children
    } = props;
    const { getLinkComponent } = useAppContext();
    const [expanded, setExpanded] = React.useState<boolean>(selected ?? false);
    const itemButtonSx = {
        minHeight: shrink ? 48 : 48,
        justifyContent: !shrink ? 'initial' : 'center',
        py: 0,
        '& :before': (level ?? 0) > 0 && !shrink ? {
            content: '""',
            display: 'block',
            position: 'absolute',
            zIndex: '1',
            left: '34px',
            height: '70%',
            width: '1px',
            opacity: '1',
            background: selected ? 'hsl(210, 100%, 60%)' : 'hsl(215, 15%, 92%)',
        } : undefined
    };
    const itemIconSx = {
        minWidth: 0,
        ml: !shrink ? 0 : 2,
        mr: !shrink ? 3 : 'auto',
        justifyContent: 'center',
    };
    const itemTextSx = {
        opacity: !shrink ? 1 : 0,
        '& span': { fontSize: '14px', fontWeight: level === 0 ? 'bold' : undefined }
    };
    const handleExpandIconClick = () => {
        setExpanded(expanded => !expanded);
    }
    const processedIcon = shrink ? icon : (children != null ? (expanded ? 'expand_more' : 'chevron_right') : icon);
    const iconComponent = processedIcon ? <ListItemIcon sx={itemIconSx}>
        <Icon fontSize={'small'}>{processedIcon}</Icon>
    </ListItemIcon> : null;
    return <>
        {(!shrink || !children) && <ListItemButton
            title={shrink ? primary : undefined}
            selected={selected}
            to={children == null ? to : undefined}
            component={children == null ? (to != null ? getLinkComponent() : undefined) : undefined}
            onClick={children != null ? handleExpandIconClick : undefined}
            sx={itemButtonSx}
            style={{ paddingLeft: ((3 + 2 * level) * 8) + 'px' }}>
            {iconComponent}
            <ListItemText primary={primary} sx={itemTextSx} />
        </ListItemButton>}
        {(shrink || expanded) && children}
    </>;
}

const ListMenuContent: React.FC<ListMenuContentProps> = (props) => {
    const {
        entries,
        level,
        shrink,
    } = props;
    const { useLocationPath } = useAppContext();
    const locationPath = useLocationPath();
    return <StyledList>
        {entries?.map((item, index) => {
            const selected = isCurrentMenuEntryOrAnyChildrenSelected(item, locationPath);
            const entryComponent = item.divider ?
                <Divider key={index} /> :
                <MenuItem
                    primary={item.title ?? ''}
                    to={item.to}
                    icon={item.icon}
                    level={level}
                    selected={selected}
                    shrink={shrink}
                    key={index}>
                    {item.children?.length ? <Box>
                        <ListMenuContent entries={item.children} level={(level ?? 0) + 1} shrink={shrink} />
                    </Box> : null}
                </MenuItem>;
            return entryComponent;
        })}
    </StyledList>;
}

const MenuTitle: React.FC<MenuTitleProps> = (props) => {
    const { title, onClose } = props;
    const theme = useTheme();
    const handleButtonClick = () => onClose?.();
    return <Box>
        <ListItemButton sx={{ backgroundColor: theme.palette.grey[200] }}>
            <ListItemIcon sx={{ minWidth: '40px' }}>
                <IconButton size="small" onClick={handleButtonClick}>
                    <Icon fontSize={'small'}>clear</Icon>
                </IconButton>
            </ListItemIcon>
            <ListItemText primary={title} sx={{ '& span': { fontWeight: 'bold' } }} />
        </ListItemButton>
        <Divider />
    </Box>;
}

export const Menu: React.FC<MenuProps> = (props) => {
    const { title, entries, onTitleClose, shrink } = props;
    return <StyledDrawer variant="permanent" open={!shrink}>
        <Box sx={{ mt: 8 }} />
        {title && <MenuTitle title={title} onClose={onTitleClose} />}
        <ListMenuContent entries={entries} shrink={shrink} />
    </StyledDrawer>;
}

export default Menu;