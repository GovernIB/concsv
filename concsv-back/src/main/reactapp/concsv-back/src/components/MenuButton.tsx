import React, { useRef } from 'react';
import { Box, Button, Icon, Menu, MenuItem } from '@mui/material';

/**
 * Botó amb menú desplegable i variant amb una llista d'accions (`MenuActionButton`).
 *
 * El botó fa servir `apiRef` per obrir i tancar el menú des de fora, i el menú es tanca
 * en triar una acció (l'acció s'executa primer i després es tanca).
 */

/** API per obrir i tancar el menú des de fora. */
export type MenuApi = {
    handleOpen: (event: React.MouseEvent<HTMLElement>) => void;
    handleClose: () => void;
};

type MenuButtonProps = {
    /** Referència amb `handleOpen` i `handleClose` per obrir i tancar el menú des de fora. */
    apiRef?: React.MutableRefObject<MenuApi>;
    id: string;
    hidden?: boolean;
    children?: any;
    buttonLabel?: string | React.ReactElement;
    buttonProps?: any;
    menuProps?: any;
    arrowDown?: string;
    arrowUp?: string;
    hiddenIcon?: boolean;
    ButtonComponent?: React.ElementType;
};

const MenuButton = (props: MenuButtonProps) => {
    const defApiRef = useRef<MenuApi>({ handleOpen: () => undefined, handleClose: () => undefined });
    const {
        apiRef = defApiRef,
        id,
        hidden,
        children,
        buttonLabel,
        buttonProps,
        menuProps,
        arrowDown = 'arrow_drop_down',
        arrowUp = 'arrow_drop_up',
        hiddenIcon = false,
        ButtonComponent = Button,
    } = props;
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);
    const handleClick = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };
    const handleClose = () => {
        setAnchorEl(null);
    };

    apiRef.current = {
        handleOpen: handleClick,
        handleClose,
    };

    if (hidden) {
        return <></>;
    }

    return (
        <>
            <ButtonComponent
                id={`menu-button-${id}`}
                aria-controls={'demo-customized-menu'}
                aria-haspopup="true"
                aria-expanded={open ? 'true' : undefined}
                onClick={handleClick}
                {...(!hiddenIcon && ButtonComponent === Button
                    ? { endIcon: <Icon sx={{ m: 0 }}>{open ? arrowUp : arrowDown}</Icon> }
                    : {})}
                {...buttonProps}
            >
                {buttonLabel}
            </ButtonComponent>
            <Menu
                id={`menu-button-${id}`}
                MenuListProps={{
                    'aria-labelledby': 'demo-customized-button',
                }}
                anchorEl={anchorEl}
                open={open}
                onClose={handleClose}
                elevation={3}
                anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'right',
                }}
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                }}
                {...menuProps}
            >
                <Box>{children}</Box>
            </Menu>
        </>
    );
};

type MenuActionButtonProps = MenuButtonProps & {
    actions: any[];
    entity?: any;
};

/**
 * Entrades de menú d'una llista d'accions. En clicar una entrada s'executa l'acció i, després, es crida
 * `handleClose` (si n'hi ha) per tancar el menú.
 */
export const actionToItem = (entity: any, actions: any[], handleClose?: () => void) => {
    return actions.map(
        (action: any, index: number) =>
            !(typeof action.hidden === 'function' ? action.hidden(entity) : action.hidden) &&
            !action?.linkTo &&
            !action?.clickShowUpdateDialog && (
                <div
                    key={`action-${index}`}
                    title={typeof action.title == 'function' ? action.title?.(entity) : action.title}
                >
                    <MenuItem
                        onClick={() => {
                            if (entity?.id != null) {
                                action?.onClick?.(entity?.id, entity);
                            } else {
                                action?.onClick?.(entity);
                            }
                            handleClose?.();
                        }}
                        disabled={typeof action?.disabled === 'function' ? action?.disabled(entity) : action?.disabled}
                    >
                        {action.icon && (
                            <Icon fontSize="small" color="action" sx={{ mr: 1 }}>
                                {action.icon}
                            </Icon>
                        )}
                        {action.label}
                    </MenuItem>
                </div>
            )
    );
};

export const MenuActionButton = (props: MenuActionButtonProps) => {
    const defApiRef = useRef<MenuApi>({ handleOpen: () => undefined, handleClose: () => undefined });
    const { apiRef = defApiRef, entity, actions, children, ...other } = props;

    const handleClose = () => {
        apiRef.current.handleClose();
    };

    return (
        <MenuButton apiRef={apiRef} {...other}>
            {actionToItem(entity, actions, handleClose)}
            {children}
        </MenuButton>
    );
};
export default MenuButton;
