
const degreesToRads = (deg: number): number => (deg * Math.PI) / 180.0;

const getGeoDistance = (
    lat1: number,
    lon1: number,
    elv1: number,
    lat2: number,
    lon2: number,
    elv2: number): number => {
    const earthR = 6371; // Radi de la terra en metres
	const latDistance = degreesToRads(lat2 - lat1);
	const lonDistance = degreesToRads(lon2 - lon1);
	const a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
				Math.cos(degreesToRads(lat1)) * Math.cos(degreesToRads(lat2)) *
				Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	const distance = earthR * c * 1000; // Converteix en metres
    return Math.sqrt(Math.pow(distance, 2) + Math.pow((elv2 - elv1), 2));
}

export const isPositionInside = (
    lat1: number,
    lon1: number,
    elv1: number,
    acc1: number,
    lat2: number,
    lon2: number,
    elv2: number,
    acc2: number): boolean => {
    return getGeoDistance(
        lat1, lon1, elv1,
        lat2, lon2, elv2) - acc1 - acc2 <= 0;
}