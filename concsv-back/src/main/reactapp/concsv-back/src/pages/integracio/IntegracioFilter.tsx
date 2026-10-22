import React from 'react';
import StyledMuiFilter from '../../components/StyledMuiFilter';
import GridFormField from '../../components/GridFormField';
import * as builder from '../../util/springFilterUtils';

/**
 * Les mides `md` sumen 11.5 perquè el bloc de botons de StyledMuiFilter (md 0.5) càpiga a la
 * mateixa fila; si se'n canvia alguna cal mantenir la suma.
 */
const IntegracioFilterForm: React.FC = () => {
    return (
        <>
            <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="dataInici" />
            <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="dataFi" />
            <GridFormField size={{ xs: 12, sm: 6, md: 3.5 }} name="descripcio" />
            <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="tipus" />
            <GridFormField size={{ xs: 12, sm: 6, md: 2 }} name="estat" />
        </>
    );
};

const springFilterBuilder = (data: any) => {
    return builder.and(
        builder.betweenDates('data', data?.dataInici, data?.dataFi),
        builder.like('descripcio', data?.descripcio),
        builder.eq('tipus', data?.tipus ? `'${data.tipus}'` : undefined),
        builder.eq('estat', data?.estat ? `'${data.estat}'` : undefined)
    );
};

export const IntegracioFilter: React.FC<any> = (props) => {
    return (
        <StyledMuiFilter
            resourceName="monitorIntegracioResource"
            code="FILTER"
            springFilterBuilder={springFilterBuilder}
            {...props}
        >
            <IntegracioFilterForm />
        </StyledMuiFilter>
    );
};

export default IntegracioFilter;
