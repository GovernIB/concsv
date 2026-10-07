import React from 'react';
import Typography, { type TypographyProps } from '@mui/material/Typography';

/** Mostra un text ressaltant les coincidències amb el filtre ràpid. */
export const TextHighlight: React.FC<{
    text?: string | null;
    match?: string;
    variant?: TypographyProps['variant'];
    ignoreCase?: boolean;
}> = ({ text, match, ignoreCase, variant = 'inherit' }) => {
    const contingut = text ?? '';
    if (!match) {
        return <Typography variant={variant}>{contingut}</Typography>;
    }
    const escapedMatch = match.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const flags = ignoreCase ? 'gi' : 'g';
    const parts = contingut.split(new RegExp(`(${escapedMatch})`, flags));
    const comparador = new RegExp(`^${escapedMatch}$`, ignoreCase ? 'i' : '');
    return (
        <Typography variant={variant}>
            {parts.map((part, index) =>
                comparador.test(part) ? (
                    <mark key={index}>{part}</mark>
                ) : (
                    <span key={index}>{part}</span>
                )
            )}
        </Typography>
    );
};

export default TextHighlight;
