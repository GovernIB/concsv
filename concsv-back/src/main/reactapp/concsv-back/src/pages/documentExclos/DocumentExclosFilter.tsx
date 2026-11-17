import React from 'react';
import StyledMuiFilter from '../../components/StyledMuiFilter';
import GridFormField from '../../components/GridFormField';
import * as builder from '../../util/springFilterUtils';

const DocumentExclosFilterForm: React.FC = () => (
    <>
        <GridFormField size={{ xs: 12, sm: 6, md: 3.5 }} name="uuid" />
        <GridFormField size={{ xs: 12, sm: 6, md: 2.5 }} name="nomDocument" />
        <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="dataCapturaInici" />
        <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="dataCapturaFi" />
        <GridFormField size={{ xs: 12, sm: 6, md: 1.5 }} name="estat" />
    </>
);

// El filtre per UUID cerca també al valor, perquè una fila pendent encara no té l'UUID resolt (i el
// valor pot ser un CSV).
type DadesFiltre = {
    uuid?: string;
    nomDocument?: string;
    dataCapturaInici?: string;
    dataCapturaFi?: string;
    estat?: string;
};

const springFilterBuilder = (data?: DadesFiltre) => {
    const identificador = data?.uuid;
    const estat = data?.estat;
    return builder.and(
        identificador
            ? builder.or(builder.like('uuid', identificador), builder.like('valor', identificador))
            : undefined,
        data?.nomDocument ? builder.like('nomDocument', data.nomDocument) : undefined,
        builder.betweenDates('dataCaptura', data?.dataCapturaInici, data?.dataCapturaFi),
        estat ? builder.eq('estatActualitzacio', `'${estat}'`) : undefined
    );
};

export const DocumentExclosFilter: React.FC<{ onSpringFilterChange?: (filter?: string) => void }> = (props) => (
    <StyledMuiFilter
        resourceName="documentExclosResource"
        code="FILTER"
        springFilterBuilder={springFilterBuilder}
        {...props}
    >
        <DocumentExclosFilterForm />
    </StyledMuiFilter>
);

export default DocumentExclosFilter;
