import { Box, Divider, Typography } from "@mui/material";
import { useAppContext } from "@programari-limit/base-react";

export type LanguageItem = {
  locale: string;
  name: string;
  flag?: string;
};

const LanguageSelectorButton = () => {
  const { currentLanguage, setCurrentLanguage } = useAppContext();
  const currentLanguageTwoChars = currentLanguage
    ? currentLanguage.substring(0, 2).toLowerCase()
    : null;

  const changeLanguage = (lang: string) => {
    setCurrentLanguage(lang);
  };

  return (
    <Box
      sx={{
        display: "flex",
        gap: 1,
        mr: 4,
        color: "black",
      }}
    >
      <Typography
        sx={{
          "&:hover": {
            cursor: currentLanguageTwoChars === "ca" ? null : "pointer",
            textDecoration:
              currentLanguageTwoChars === "ca" ? "none" : "underline",
          },
          fontWeight: currentLanguageTwoChars === "ca" ? "bold" : "normal",
          fontSize: "20px",
        }}
        onClick={() => changeLanguage("ca")}
      >
        CA
      </Typography>
      <Divider orientation="vertical" flexItem />
      <Typography
        sx={{
          "&:hover": {
            cursor: currentLanguageTwoChars === "es" ? null : "pointer",
            textDecoration:
              currentLanguageTwoChars === "es" ? "none" : "underline",
          },
          fontWeight: currentLanguageTwoChars === "es" ? "bold" : "normal",
          fontSize: "20px",
        }}
        onClick={() => changeLanguage("es")}
      >
        ES
      </Typography>
    </Box>
  );
};
export default LanguageSelectorButton;
