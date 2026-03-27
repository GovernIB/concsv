const translationCa = {
    title: "Consulta CSV",
    language: {
        ca: "Català",
        es: "Castellà",
    },
    menu: {
        home: "Inici",
        a11y: "Accessibilitat",
    },
    page: {
        notFound: {
            title: "No trobat",
            description: "Revisi el valor introduït a la barra d'adreces",
        },
        home: {
            title: "Validació amb codi CSV",
            p1: "El codi segur de verificació (CSV) és un sintagma que designa el codi únic que identifica un document electrònic en l'Administració pública espanyola. Aquest codi alfanumèric sol aparèixer en tots el documents electrònics emesos per mitjans telemàtics. El CSV permet comprovar la integritat i l'autenticitat del document (article 27 de la Llei 39/2015, d'1 d'octubre, del procediment administratiu comú de les administracions públiques).",
            p2: "Aquesta aplicació permet verificar la validesa i la integritat d'un document electrònic emès pel Govern de les Illes Balears. Escriviu el CSV que apareix als marges del document per tornar a visualitzar-lo i comprovar que les dades que s'hi consignen són correctes. Una vegada formalitzat i tramès el formulari, es mostrarà la informació del document per poder descarregar-lo.",
            placeholder: "Introdueixi el codi CSV",
            validate: "Validar",
            invalid: {
                empty: "És obligatori especificar un valor",
                length: "El codi ha de tenir un mínim de 32 caràcters"
            }
        },
        view: {
            loading: {
                title: "Cercant el document...",
            },
            error: {
                title: "Error en la consulta",
                description: "S'ha produït un error al consultar el document",
                cause: "Torna-ho a provar d'aquí uns instants.",
                back: "Tornar a intentar",
            },
            found: {
                title: "Document trobat",
                download: {
                    original: "Document original",
                    printable: "Còpia autèntica",
                    preview: "Previsualitzar",
                    notavailable: "La versió imprimible no està disponible per a aquest document",
                    hasPassword: "Aquest document està protegit amb contrasenya i, per això, no es pot generar la còpia autèntica imprimible del document ni obtenir les dades de les signatures. En aquest cas, pot descarregar la versió original.",
                    malformatted: "Aquest document PDF està mal format o conté errors i no es pot generar la còpia autèntica imprimible. Tot i així, pot descarregar la versió original i consultar les signatures que hi conté.",
                },
                preview: {
                    error1: "S'han produït errors al previsualitzar la versió imprimible. En el seu lloc es mostra la versió original.",
                    error2: "No s'ha pogut generar la previsualització del document.",
                },
                firmants: {
                    title: "Firmants",
                    error: "S'han produït errors obtenint els firmants del document",
                    representat: {
                        part1: "En representació de",
                        part2: "amb NIF / CIF",
                    },
                    dataSignatura: {
                        title: "Data signatura",
                        comment: "La data de signatura és la que tenia l'ordinador de l'usuari quan va firmar",
                        order: "L'ordre de les firmes és per data de signatura descendent, de la més nova a la més antiga. En el cas que no es disposi de la data, l'ordenació és per nom dels firmants.",
                    },
                    rao: "Raó",
                    segell: "Firma amb segell de temps",
                    csv: "El document té el perfil de firma TF01 CSV i tot i estar guardat com a definitiu no s'ha reconegut cap firma vàlida"
                },
                metadades: {
                    title: "Metadades ENI del document",
                    nom: "Nom del document",
                    csv: "Codi segur de verificació",
                    identificador: "Identificador del document",
                    tipusDoc: "Tipus de document",
                    estat: "Estat d'elaboració",
					eni: {
						tipus: {
							TD01: "Resolució",
							TD02: "Acord",
							TD03: "Contracte",
							TD04: "Conveni",
							TD05: "Declaració",
							TD06: "Comunicació",
							TD07: "Notificació",
							TD08: "Publicació",
							TD09: "Acusament de rebut",
							TD10: "Acta",
							TD11: "Certificat",
							TD12: "Diligència",
							TD13: "Informe",
							TD14: "Solicitud",
							TD15: "Denúncia",
							TD16: "Alegació",
							TD17: "Recursos",
							TD18: "Comunicació ciutadà",
							TD19: "Factura",
							TD20: "Altres incautats",
							TD99: "Altres"
						},
						estatElaboracio: {
							EE01: "Original",
							EE02: "Còpia electrònica autèntica amb canvi de format",
							EE03: "Còpia electrònica autèntica de document paper",
							EE04: "Còpia electrònica parcial autèntica",
							EE99: "Altres"
						},
						signaturaTipus: {
						    TF01: "CSV",
							TF02: "XAdES internally detached signature",
							TF03: "XAdES enveloped signature",
							TF04: "CAdES detached/explicit signature",
							TF05: "CAdES attached/implicit signature",
							TF06: "Pades"
						},
						origen: {
						    0: "Ciutadà",
						    1: "Administració"
						}
					},
                    versio: "Versió NTI",
                    organ: "Òrgan",
                    origen: "Origen",
                    tipusSig: "Tipus de signatura",
                    dataCaptura: "Data captura",
                    evidencies: "Les evidències que garanteixen l'autenticitat, integritat i conservació a llarg termini del document es troben al gestor documental de la CAIB"
                }
            },
            notFound: {
                title: "Document no trobat",
                description: "Possiblement hi ha hagut un error introduïnt el codi CSV. Tornau-ho a intentar.",
                back: "Tornar a intentar",
            },
        },
        a11y: {
            title: "Declaració d'accessibilitat",
            reialDecret: "Reial Decret 1112/2018",
            p11: "El Govern de les Illes Balears s'ha compromès a fer accessible el seu lloc web i la seva aplicació per a dispositius mòbils, de conformitat amb el",
            p12: "de 7 de setembre, d'accessibilitat dels llocs web i aplicacions mòbils del sector públic.",
            p21: "La present declaració d'accessibilitat s'aplica al lloc web",
            p22: "i exclou les pàgines que condueixen a enllaços externs.",
            situacio: {
                title: "Situació de compliment",
                p11: "Aquest lloc web és parcialment conforme amb el",
                p12: ", a causa de les excepcions i de la manca de conformitat dels aspectes que s'indiquen a continuació.",
                contingut: {
                    title: "Contingut no accessible",
                    p1: "El contingut que es recull a continuació no és accessible pels següents motius:",
                    m1: "Manca de conformitat amb el",
                    m11: "No es pot assegurar que es pugui visualitzar i operar amb tots els continguts en qualsevol tipus d'orientació de la pantalla [requisit 9.1.3.4 - Orientació d'UNE-EN 301549:2019].",
                    m12: "És possible que no es proporcionin etiquetes o instruccions quan el contingut requereix la introducció de dades per part de l'usuari [requisit 9.3.3.2 - Etiquetes o instruccions d'UNE-EN 301549:2019].",
                    m2: "El contingut no entra dins l'àmbit de la legislació aplicable, atès que és intrínsecament no accessible:",
                    m21: "Els mapes. S'han proporcionat alternatives per pal·liar aquest problema, com ara les adreces.",
                }
            },
            preparacio: {
                title: "Preparació de la present declaració d'accessibilitat",
                p1: "La present declaració va ser preparada el 09 de novembre de 2022.",
                p2: "El mètode emprat per preparar la declaració ha estat una autoavaluació duita a terme pel mateix organisme.",
                p3: "Darrera revisió de la declaració: 09 de novembre de 2022.",
            },
            observacions: {
                title: "Observacions i dades de contacte",
                p11: "El Govern de les Illes Balears pretén continuar millorant i oferir als ciutadans el millor servei possible. Pot realitzar comunicacions sobre requisits d'accessibilitat (article 10.2.a) del",
                p12: "com ara:",
                o1: "Informar sobre qualsevol possible incompliment per part d'aquest lloc web.",
                o2: "Transmetre altres dificultats d'accés a l'contingut.",
                o3: "Formular qualsevol altra consulta o suggeriment de millora relativa a l'accessibilitat al lloc web.",
                p21: "Mitjançant el següent",
                form: {
                    title: "formulari de contacte",
                    url: "https://www.caib.es/sistrafront/sistrafront/inicio?language=ca&modelo=PD0018ENCW&version=2",
                },
                p22: "o trucant a al telèfon 971177140, pot presentar:",
                o4: "Una queixa relativa al compliment dels requisits del",
                o5: "Una sol·licitud d'informació accessible relativa a:",
                o511: "Continguts que estan exclosos de l'àmbit d'aplicació del",
                o512: "segons el que estableix l'article 3, apartat 4.",
                o52: "Continguts que estan exempts de compliment dels requisits d'accessibilitat per imposar una càrrega desproporcionada.",
                p3: "Mitjançant el següent procediment:",
                procediment: {
                    name: "Peticions d'informació accessible i queixes relatives a accessibilitat de llocs web i aplicacions mòbils",
                    url: "https://www.caib.es/seucaib/ca/200/personas/tramites/tramite/4055271",
                    title: "Procediment d'aplicació",
                    p11: "El procediment de reclamació que recull l'article 13 del",
                    p12: "va entrar en vigor el 20 de setembre de 2020.",
                    p2: "Si un cop realitzada una sol·licitud d'informació accessible o queixa, aquesta hagués estat desestimada, no s'estigués d'acord amb la decisió adoptada, o la resposta no complís els requisits previstos en l'article 12.5, la persona interessada podrà iniciar una reclamació. Igualment es podrà iniciar una reclamació en el cas que hagi passat el termini de vint dies hàbils sense haver obtingut resposta.",
                    p3: "La reclamació pot ser presentada a través del procediment",
                    reclamacio: {
                        title: "Reclamació relativa a accessibilitat de llocs web i aplicacions mòbils",
                        url: "https://www.caib.es/seucaib/ca/200/personas/tramites/tramite/4057494",
                    },
                },
            },
        },
    },
    footer: {
        fechaCompilacion: "Data de compilació:",
        revision: "Revisió:",
        versionJDK: "Versió de JDK:",
        titleVersion: "Informació de la versió",
    },
};

export default translationCa;