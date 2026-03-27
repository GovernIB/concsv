const translationEs = {
    title: "Consulta CSV",
    language: {
        ca: "Catalán",
        es: "Castellano",
    },
    menu: {
        home: "Inicio",
        a11y: "Accesibilidad",
    },
    page: {
        notFound: {
            title: "No encontrado",
            description: "Revise el valor introducido em la barra de direcciones",
        },
        home: {
            title: "Validación con código CSV",
            p1: "El código seguro de verificación (CSV) es un sintagma que designa el código único que identifica un documento electrónico en la Administración pública española. Este código alfanumérico suele aparecer en todos los documentos electrónicos emitidos por medios telemáticos. El CSV permite comprobar la integridad y autenticidad del documento (artículo 27 de la Ley 39/2015, de 1 de Octubre, del procedimiento administrativo común de las administraciones públicas).",
            p2: "Esta aplicación permite verificar la validez y la integridad de un documento electrónico emitido por el Gobierno de las Islas Baleares. Escriba el CSV que aparece en los márgenes del documento para visualizarlo de nuevo y comprobar que los datos consignados son correctos. Una vez formalizado y enviado el formulario, se mostrará la información del documento para poder descargarlo.",
            placeholder: "Introduzca el código CSV",
            validate: "Validar",
            invalid: {
                empty: "Es obligatorio especificar un valor",
                length: "El código debe tener un mínimo de 32 carácteres"
            }
        },
        view: {
            loading: {
                title: "Buscando el documento...",
            },
            error: {
                title: "Error en la consulta",
                description: "Se ha producido un error al consultar el documento",
                solution: "Vuelva a intentarlo transcurridos un tiempo.",
                back: "Volver a intentar",
            },
            found: {
                title: "Documento encontrado",
                download: {
                    original: "Documento original",
                    printable: "Copia auténtica",
                    preview: "Previsualizar",
                    notavailable: "La versión imprimible no está disponible para este documento",
                    hasPassword: "Este documento está protegido con contraseña y, por eso, no se puede generar la copia auténtica imprimible del documento ni obtener los datos de las firmas. En este caso, puede descargar la versión original.",
                    malformatted: "Este documento PDF está mal formado o contiene errores y no puede generarse la copia auténtica imprimible. Sin embargo, puede descargar la versión original y consultar las firmas que contiene.",
                },
                preview: {
                    error1: "Se han producido errores el previsualitzar la versión imprimible. En su lugar se muestra la versión original.",
                    error2: "No se ha podido generar la previsualización del documento.",
                },
                firmants: {
                    title: "Firmantes",
                    error: "Se han producido errores obteniendo los firmates del documento",
                    representat: {
                        part1: "En representación de",
                        part2: "con NIF / CIF",
                    },
                    dataSignatura: {
                        title: "Fecha firma",
                        comment: "La fecha de firma es la que tenía el ordenador del usuario en el momento de la firma",
                        order: 'El orden de las firmas es por fecha de firma descendente, de la más nueva a la más antigua. En el supuesto de que no se disponga de la fecha, la ordenación es por nombre de los firmantes.',
                    },
                    rao: "Razón",
                    segell: "Firma con sello de tiempo",
                    csv: "El documento tiene el perfil de firma TF01 CSV y pese a estar guardado como definitivo no se ha reconocido ninguna firma válida"
                },
                metadades: {
                    title: "Metadatos ENI del documento",
                    nom: "Nombre del documento",
                    csv: "Código seguro de verificación",
                    identificador: "Identificador del documento",
                    tipusDoc: "Tipo de documento",
                    estat: "Estado de elaboración",
					eni: {
						tipus: {
							TD01: "Resolución",
							TD02: "Acuerdo",
							TD03: "Contrato",
							TD04: "Convenio",
							TD05: "Declaración",
							TD06: "Comunicación",
							TD07: "Notificación",
							TD08: "Publicación",
							TD09: "Acuse de recibo",
							TD10: "Acta",
							TD11: "Certificado",
							TD12: "Diligencia",
							TD13: "Informe",
							TD14: "Solicitud",
							TD15: "Denuncia",
							TD16: "Alegación",
							TD17: "Recursos",
							TD18: "Comunicación ciudadano",
							TD19: "Factura",
							TD20: "Otros incautados",
							TD99: "Otros"
						},
						estatElaboracio: {
							EE01: "Original",
							EE02: "Copia electrónica auténtica con cambio de formato",
							EE03: "Copia electrónica auténtica de documento papel",
							EE04: "Copia electrónica parcial auténtica",
							EE99: "Otros"
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
						    0: "Ciudadano",
						    1: "Administración"
						}
					},
                    versio: "Versión NTI",
                    organ: "Órgano",
                    origen: "Orígen",
                    tipusSig: "Tipo de firma",
                    dataCaptura: "Fecha captura",
                    evidencies: "Las evidencias que garantizan la autenticidad, integridad y conservación a largo plazo del documento se encuentran en el gestor documental de la CAIB"
                }
            },
            notFound: {
                title: "Documento no encontrado",
                description: "Posiblemente ha habido algun error al introducir el código CSV. Vuelva a intentarlo",
                back: "Volver a intentar",
            },
        },
        a11y: {
            title: "Declaración de accesibilidad",
            reialDecret: "Real Decreto 1112/2018",
            p11: "El Govern de les Illes Balears se ha comprometido a hacer accesible su sitio web y su aplicación para dispositivos móviles, de conformidad con el",
            p12: "de 7 de septiembre, de accesibilidad de los sitios web y aplicaciones móviles del sector público.",
            p21: "La presente declaración de accesibilidad se aplica el sitio web",
            p22: " y excluye las páginas que nos llevan a enlaces externos.",
            situacio: {
                title: "Situación de cumplimiento",
                p11: "Este sitio web es parcialmente conforme con el",
                p12: ", debido a las excepciones y a la falta de conformidad del aspectos que se indican a continuación.",
                contingut: {
                    title: "Contenido no accesible",
                    p1: "El contenido que se recoge a continuación no es accesible por lo siguiente:",
                    m1: "Falta de conformidad con el",
                    m11: "No se puede asegurar que se pueda visualizar y operar con todos los contenidos en cualquier tipo de orientación de la pantalla [requisito 9.1.3.4 - Orientación de UNE-EN 301549:2019].",
                    m12: "Es posible que no se proporcionen etiquetas o instrucciones cuando el contenido requiere la introducción de datos por parte del usuario [requisito 9.3.3.2 - Etiquetas o instrucciones de UNE-EN 301549:2019].",
                    m2: "El contenido no entra dentro del ámbito de la legislación aplicable, al ser contenidos intrínsecamente no accesibles:",
                    m21: "Los mapas. Se han proporcionado alternativas para paliar este problema como las direcciones.",
                }
            },
            preparacio: {
                title: "Preparación de la presente declaración de accesibilidad",
                p1: "La presente declaración fue preparada el 09 de noviembre de 2022.",
                p2: "El método empleado para preparar la declaración ha sido una autoevaluación llevada a cabo por el propio organismo.",
                p3: "Última revisión de la declaración: 09 de noviembre de 2022.",
            },
            observacions: {
                title: "Observaciones y datos de contacto",
                p11: "El Govern de las Illes Balears pretende seguir mejorando y ofrecer a los ciudadanos el mejor servicio posible. Puede realizar comunicaciones sobre requisitos de accesibilidad (artículo 10.2.a) del",
                p12: "como por ejemplo:",
                o1: "Informar sobre cualquier posible incumplimiento por parte de este sitio web",
                o2: "Transmitir otras dificultades de acceso al contenido.",
                o3: "Formular cualquier otra consulta o sugerencia de mejora relativa a la accesibilidad del sitio web.",
                p21: "A través del siguiente",
                form: {
                    title: "formulario de contacto",
                    url: "https://www.caib.es/sistrafront/sistrafront/inicio?language=es&modelo=PD0018ENCW&version=2",
                },
                p22: "o llamando al teléfono 971177140, puede presentar:",
                o4: "Una queja relativa al cumplimiento de los requisitos del",
                o5: "Una solicitud de información accesible relativa a: ",
                o511: "Contenidos que están excluidos del ámbito de aplicación del",
                o512: "según lo establecido por el artículo 3, apartado 4.",
                o52: "Contenidos que están exentos del cumplimiento de los requisitos de accesibilidad por imponer una carga desproporcionada.",
                p3: "A través del siguiente procedimiento:",
                procediment: {
                    name: "Peticiones de información accesible y quejas relativas a accesibilidad de sitios web y aplicaciones móviles",
                    url: "https://www.caib.es/seucaib/es/200/personas/tramites/tramite/4055271",
                    title: "Procedimiento de aplicación",
                    p11: "El procedimiento de reclamación recogido en el artículo 13 del",
                    p12: "entró en vigor el 20 de septiembre de 2020.",
                    p2: "Si una vez realizada una solicitud de información accesible o queja, ésta hubiera sido desestimada, no se estuviera de acuerdo con la decisión adoptada, o la respuesta no cumpliera los requisitos contemplados en el artículo 12.5, la persona interesada podrá iniciar una reclamación. Igualmente se podrá iniciar una reclamación en el caso de que haya trascurrido el plazo de veinte días hábiles sin haber obtenido respuesta.",
                    p3: "La reclamación puede ser presentada a través del procedimiento",
                    reclamacio: {
                        title: "Reclamaciones relativa a accesibilidad de sitios web y aplicaciones móviles",
                        url: "https://www.caib.es/seucaib/es/200/personas/tramites/tramite/4057494",
                    },
                }
            },
        },
    },
    footer: {
        fechaCompilacion: "Fecha de compilación:",
        revision: "Revisión:",
        versionJDK: "Versión de JDK:",
        titleVersion: "Información de la versión",
    },
};

export default translationEs;