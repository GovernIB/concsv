import React from 'react';
import StyledMuiFilter from '../../components/StyledMuiFilter';
import GridFormField from '../../components/GridFormField';
import * as builder from '../../util/springFilterUtils';

const IntegracioFilterForm: React.FC = () => {
    return (
        <>
            <GridFormField size={{ xs: 12, sm: 6, md: 1.9 }} name="dataInici" />
            <GridFormField size={{ xs: 12, sm: 6, md: 1.9 }} name="dataFi" />
            <GridFormField size={{ xs: 12, sm: 6, md: 3.2 }} name="descripcio" />
            <GridFormField size={{ xs: 12, sm: 6, md: 2.1 }} name="tipus" />
            <GridFormField size={{ xs: 12, sm: 6, md: 2.15 }} name="estat" />
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
