import React from 'react';
import StyledMuiFilter from '../../components/StyledMuiFilter';
import GridFormField from '../../components/GridFormField';
import * as builder from '../../util/springFilterUtils';

const CacheDocumentFilterForm: React.FC = () => (
    <>
        <GridFormField size={{ xs: 12, sm: 6, md: 2.5 }} name="carpeta" />
        <GridFormField size={{ xs: 12, sm: 6, md: 3.5 }} name="identificador" />
        <GridFormField size={{ xs: 12, sm: 6, md: 2.5 }} name="caducat" />
    </>
);

// "caducat" és un Boolean, que el motor de recursos representa amb un desplegable de tres valors
// (buit / Sí / No), com "activa" a EntitatFilter. Cal comparar-lo explícitament amb null i amb la
// cadena buida: amb una comprovació de veritat, triar "No" (false) es confondria amb "sense filtrar".
const springFilterBuilder = (data?: { carpeta?: string; identificador?: string; caducat?: boolean | '' }) => {
    const caducat = data?.caducat;
    return builder.and(
        builder.eq('carpeta', data?.carpeta ? `'${data.carpeta}'` : undefined),
        data?.identificador ? builder.like('identificador', data.identificador) : undefined,
        caducat != null && (caducat as unknown) !== '' ? builder.eq('caducat', caducat) : undefined
    );
};

export const CacheDocumentFilter: React.FC<{ onSpringFilterChange?: (filter?: string) => void }> = (props) => (
    <StyledMuiFilter
        resourceName="cacheDocumentResource"
        code="FILTER"
        springFilterBuilder={springFilterBuilder}
        {...props}
    >
        <CacheDocumentFilterForm />
    </StyledMuiFilter>
);

export default CacheDocumentFilter;
