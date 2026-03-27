import React from 'react';
import MuiDialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import { DialogButton, MessageDialogShowFn } from '../AppContext';
import DialogButtons from './DialogButtons';

export type DialogProps = React.PropsWithChildren & {
    open: boolean;
    closeCallback: () => void;
    title?: string | null;
    buttons?: DialogButton[];
    buttonCallback?: (value: any) => void;
    componentProps?: any;
};

export const useMessageDialog: (() => [MessageDialogShowFn, React.ReactElement]) = () => {
    const [open, setOpen] = React.useState<boolean>(false);
    const [title, setTitle] = React.useState<string | null>();
    const [message, setMessage] = React.useState<string>();
    const [buttons, setButtons] = React.useState<DialogButton[]>([]);
    const [componentProps, setComponentProps] = React.useState<any>();
    const [resolveFn, setResolveFn] = React.useState<(value: any) => void>();
    const showDialog: MessageDialogShowFn = (title: string | null, message: string, buttons: DialogButton[], componentProps?: any) => {
        setTitle(title);
        setMessage(message);
        setButtons(buttons);
        setComponentProps(componentProps);
        setOpen(true);
        return new Promise<string>((resolve) => {
            setResolveFn(() => resolve);
        });
    }
    const buttonOrCloseCallback = (value?: any) => {
        setOpen(false);
        resolveFn?.(value);
    }
    const dialogComponent = <Dialog
        open={open}
        buttonCallback={buttonOrCloseCallback}
        closeCallback={buttonOrCloseCallback}
        title={title}
        buttons={buttons}
        componentProps={componentProps}>
        {message && <DialogContentText>{message}</DialogContentText>}
    </Dialog>;
    return [showDialog, dialogComponent];
}

export const Dialog: React.FC<DialogProps> = (props) => {
    const {
        open,
        buttonCallback,
        closeCallback,
        title,
        buttons,
        componentProps,
        children,
    } = props;
    return <MuiDialog
        open={open}
        onClose={() => closeCallback()}
        {...componentProps}>
        {title && <DialogTitle>{title}</DialogTitle>}
        <DialogContent>{children}</DialogContent>
        {buttons && <DialogButtons buttons={buttons} handleClose={(value: any) => buttonCallback?.(value)} />}
    </MuiDialog>;
}

export default Dialog;