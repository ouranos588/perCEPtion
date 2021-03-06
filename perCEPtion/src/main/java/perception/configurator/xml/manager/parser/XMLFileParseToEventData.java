package perception.configurator.xml.manager.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import perception.configurator.xml.enums.general.FileErrorType;
import perception.configurator.xml.enums.general.XMLFileStructure;
import perception.configurator.xml.enums.parser.ParsingErrorType;
import utils.Pair;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe abstraite permettant le parsing de fichiers de configuration XML pour les events présentant
 * plusieurs paramètres.
 *
 * @author Chloé GUILBAUD, Léo PARIS, Kendall FOREST, Mathieu GUYOT
 */
public abstract class XMLFileParseToEventData {

    // Element commun aux parsers
    private static String xmlNodeParamAttrType = XMLFileStructure.EVENT_PARAM_ATTR_TYPE.getLabel();

    // Element propre aux parsers courant

    // La balise du fichier XML définisant un event
    // ex : simple
    private String xmlNodeSingularEventLabel;

    // Erreur de parsing propre à l'event
    private ParsingErrorType parsingErrorType_pluralEventLabel;
    private ParsingErrorType parsingErrorType_pluralEventDuplicated;
    private ParsingErrorType parsingErrorType_pluralEventInvalidName;
    private ParsingErrorType parsingErrorType_pluralEventInvalidType;

    public XMLFileParseToEventData(
            //XMLFileParserElement elementToParse,
            String xmlNodeSingularEventLabel,
            ParsingErrorType parsingErrorType_pluralEventLabel,
            ParsingErrorType parsingErrorType_pluralEventDuplicated,
            ParsingErrorType parsingErrorType_pluralEventInvalidName,
            ParsingErrorType parsingErrorType_pluralEventInvalidType) {
        //this.elementToParse = elementToParse;
        this.xmlNodeSingularEventLabel = xmlNodeSingularEventLabel;
        this.parsingErrorType_pluralEventLabel = parsingErrorType_pluralEventLabel;
        this.parsingErrorType_pluralEventDuplicated = parsingErrorType_pluralEventDuplicated;
        this.parsingErrorType_pluralEventInvalidName = parsingErrorType_pluralEventInvalidName;
        this.parsingErrorType_pluralEventInvalidType = parsingErrorType_pluralEventInvalidType;
    }

    /**
     * Extrait les informations pour l'instanciation des events.
     * Permet de passer d'un fichier XML à des objets métiers.
     *
     * @return {@link ResultatParsing} comprenant les informations résultant du traitement du fichier, de sa validation
     * et le tableau associatif permettant l'instanciation des events
     * @throws ParserConfigurationException {@link ParserConfigurationException}
     * @throws IOException                  {@link IOException}
     * @throws SAXException                 {@link SAXException}
     */
    public ResultatParsing parse(String filePath)
            throws ParserConfigurationException, SAXException, IOException {

        // Initialisation de l'objet résultant du parsing
        ResultatParsing resultatParsing = ResultatParsing.FAB();

        // Récupération d'une instance de factory qui fournira un parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Parsing du fichier xml via un objet File et récupération d'un objet
        // Document qui permet de représenter la hiérarchie d'objet créée pendant le
        // parsing
        Pair<Boolean, Document> createdParser = XMLFileParseToEventData.createParser(factory, filePath, resultatParsing);
        Boolean test = createdParser.getFirst();
        Document xml = createdParser.getSecond();

        // Si le ficher est introuvable, le parsing est arrêté
        if (test) {
            // Récupération d'un objet Element qui représente un élément XML
            // Ici, cet élément sera la racine du document
            Element root = xml.getDocumentElement();

            // Récupération d'une instance de factory qui fournira un objet
            // permettant d'utiliser le languge xpath
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();

            this.parseEvents(xPath, root, resultatParsing);
        }

        return resultatParsing;

    }

    /**
     * Permet la création du parser de fichier XML
     *
     * @param factory         {@link DocumentBuilderFactory}
     * @param filePath        le chemin vers le fichier de configuration XML
     * @param resultatParsing le résultat du parsing
     * @return un tuble comprenant
     * - un boolean à vrai si le parsing a été possible et false dans le cas contraire
     * - l'objet représentatif du fichier XML
     * @throws ParserConfigurationException {@link ParserConfigurationException}
     * @throws IOException                  {@link IOException}
     * @throws SAXException                 {@link SAXException}
     */
    public static Pair<Boolean, Document> createParser(
            DocumentBuilderFactory factory, String filePath, ResultatParsing resultatParsing)
            throws ParserConfigurationException, IOException, SAXException {

        Document xml = null;
        boolean test = true;

        try {
            // Création du parser
            DocumentBuilder builder = factory.newDocumentBuilder();
            File fileXML = new File(filePath);

            xml = builder.parse(fileXML);

        } catch (FileNotFoundException ex) {
            resultatParsing.addFileErrorType(FileErrorType.FILE_NOT_FOUND);
            ex.printStackTrace();
            test = false;
        }

        return new Pair<Boolean, Document>(test, xml);

    }

    /**
     * Parse tous les events du fichier XML fourni de configuration des évènements du sytème. Un
     * {@link ResultatParsing} est passé en paramètre et sera mis à jour au cours du traitement.
     *
     * @param xPath           - le xPath
     * @param root            - la racine du fichier XML de configuration des modules du système
     * @param resultatParsing - le résultat du parsing qui sera mis à jour au cours du traitement
     */
    protected void parseEvents(XPath xPath, Element root, ResultatParsing resultatParsing) {

        Optional<NodeList> eventFromFileOp = getEventsInFile(xPath, root, resultatParsing);

        // Si la liste est absente c'est que le fichier ne comporte pas de events
        eventFromFileOp.ifPresent(nodeList -> createAllEvents(xPath, nodeList, resultatParsing));

    }

    /**
     * Récupération de tous les events dans le fichier XML fourni. Un {@link ResultatParsing} est passé en
     * paramètre et sera mis à jour au cours du traitement.
     *
     * @param xPath           le xPath
     * @param root            l'élément racine du fichier XML
     * @param resultatParsing le résultat du parsing qui sera mis à jour au cours du traitement
     * @return un optional contenant éventuellement la liste de events. Il est vide si le fichier n'en comporte pas, dans ce cas, le
     * {@link ResultatParsing} est mis à jour
     */
    protected Optional<NodeList> getEventsInFile(XPath xPath, Element root, ResultatParsing resultatParsing) {

        // Récupération de tout les events du fichier avec XPath
        String expXPathJeuxDeDonnees = "//" + xmlNodeSingularEventLabel;
        Optional<NodeList> listEventOp = Optional.empty();
        try {
            NodeList listPrimitiveEvent = (NodeList) xPath.evaluate(expXPathJeuxDeDonnees, root, XPathConstants.NODESET);
            listEventOp = Optional.of(listPrimitiveEvent);
        } catch (XPathExpressionException e) {
            resultatParsing.addParsingErrorType(parsingErrorType_pluralEventLabel);
            e.printStackTrace();
        }

        return listEventOp;

    }

    /**
     * Création de toutes les informations permettant l'instanciation des events à partir du fichier XML.
     *
     * @param xPath                    le xPath
     * @param listEventsFromFile       la liste des events du fichier
     * @param resultatParsing          le résultat du parsing qui sera mis à jour au cours du traitement, dans ce cas,
     * le {@link ResultatParsing} est mis à jour
     */
    protected void createAllEvents(XPath xPath, NodeList listEventsFromFile,
                                   ResultatParsing resultatParsing) {

        for (int i = 0; i < listEventsFromFile.getLength(); i++) {

            Node node = listEventsFromFile.item(i);

            Optional<String> eventName = Optional.empty();
            Optional<String> eventType = Optional.empty();
            Optional<List<Pair<String, String>>> eventParamList = Optional.empty();

            // Récupération des éléments du event actuel
            boolean primitiveEventEnabled = XMLFileParseToEventData.isEnabledEvent(xPath, node);
            if (primitiveEventEnabled) {
                eventName = getEventNameFromFile(xPath, node, resultatParsing);
                eventType = getEventTypeFromFile(xPath, node, resultatParsing);
                eventParamList = getEventParamListFromFile(xPath, node, resultatParsing);
            }

            // Si on a aucune erreur dans le fichier les informations d'instanciation du event courant est
            // ajouté au résultat du parsing
            if (eventName.isPresent() && eventType.isPresent() && eventParamList.isPresent()) {
                this.addEventData(eventName.get(), eventType.get(), eventParamList.get(), resultatParsing);
            }

        }

    }

    /**
     * Récupére le nom donné dans le fichier XML pour le event spécifié.
     *
     * @param xPath           le XPath
     * @param node            le noeud dans le fichier correspondant au event
     * @param resultatParsing le résultat du parsing qui sera mis à jour au cours du traitement
     * @return un optional contenant le nom du event ou étant vide s'il est impossible de trouver l'information
     * dans le fichier, dans ce cas, le {@link ResultatParsing} est mis à jour
     */
    protected Optional<String> getEventNameFromFile(XPath xPath, Node node, ResultatParsing resultatParsing) {
        Optional<String> nameOp = Optional.empty();
        try {
            String strSelectName = XMLFileStructure.EVENT_NAME.getLabel();
            String name = "" + xPath.evaluate(strSelectName, node, XPathConstants.STRING);
            if (name.equals("")) {
                throw new XPathExpressionException("Missing event name.");
            } else if (existingEventWithNameInResultatParsing(name, resultatParsing)) {
                resultatParsing.addParsingErrorTypeWithComplementMessage(parsingErrorType_pluralEventDuplicated, name);
            } else {
                nameOp = Optional.of(name);
            }
        } catch (XPathExpressionException e) {
            resultatParsing.addParsingErrorType(parsingErrorType_pluralEventInvalidName);
            // System.out.println("Impossible de trouver le nom du event : " + node);
            e.printStackTrace();
        }
        return nameOp;
    }

    /**
     * Récupére le type donnée dans le fichier XML pour le event spécifié.
     *
     * @param xPath           le XPath
     * @param node            le noeud dans le fichier correspondant au event
     * @param resultatParsing le résultat du parsing qui sera mis à jour au cours du traitement
     * @return un optional contenant le nom du event ou étant vide s'il est impossible de trouver l'information
     * dans le fichier, dans ce cas, le {@link ResultatParsing} est mis à jour
     */
    protected Optional<String> getEventTypeFromFile(XPath xPath, Node node, ResultatParsing resultatParsing) {
        Optional<String> typeOp = Optional.empty();
        try {
            String strSelectName = XMLFileStructure.EVENT_TYPE.getLabel();
            String type = "" + xPath.evaluate(strSelectName, node, XPathConstants.STRING);
            if (type.equals("")) {
                throw new XPathExpressionException("Missing event type.");
            } else {
                typeOp = Optional.of(type);
            }
        } catch (XPathExpressionException e) {
            resultatParsing.addParsingErrorType(parsingErrorType_pluralEventInvalidType);
            // System.out.println("Impossible de trouver le nom du event : " + node);
            e.printStackTrace();
        }
        return typeOp;
    }

    /**
     * Récupération de tous les params events dans le fichier XML fourni. Un {@link ResultatParsing} est passé en
     * paramètre et sera mis à jour au cours du traitement.
     *
     * @param xPath           le xPath
     * @param node            le noeud corespondant à un event du fichier XML de configuration
     * @param resultatParsing le résultat du parsing qui sera mis à jour au cours du traitement
     * @return un optional contenant éventuellement la liste d'events. Il est vide si le fichier n'en comporte pas, dans ce cas, le
     * {@link ResultatParsing} est mis à jour
     */
    protected Optional<List<Pair<String, String>>> getEventParamListFromFile(XPath xPath, Node node, ResultatParsing resultatParsing) {

        // Récupération de tout les events du fichier avec XPath
        String expXPathJeuxDeDonnees = XMLFileStructure.EVENT_PARAMS.getLabel() + "/" + XMLFileStructure.EVENT_PARAM.getLabel();
        Optional<List<Pair<String, String>>> listParamEventOp = Optional.empty();
        try {
            NodeList listParamEventNode = (NodeList) xPath.evaluate(expXPathJeuxDeDonnees, node, XPathConstants.NODESET);
            List<Pair<String, String>> eventParamList = getEventParams(xPath, listParamEventNode, resultatParsing);
            listParamEventOp = Optional.of(eventParamList);

        } catch (XPathExpressionException e) {
            resultatParsing.addParsingErrorType(parsingErrorType_pluralEventLabel);
            e.printStackTrace();
        }

        return listParamEventOp;

    }

    /**
     * Permet la récupération des paramètres pour l'event à partir des informations du fichier XML de configuration.
     *
     * @param xPath                    le xPath
     * @param listParamsEventsFromFile la liste des paramètres de l'event
     * @param resultatParsing          le résultat de parsing qui sera mit à jour au cours du traitement en cas d'erreur de parsing
     * @return une liste de tuples comportant le type et la valeur du paramètre
     */
    protected List<Pair<String, String>> getEventParams(XPath xPath, NodeList listParamsEventsFromFile,
                                                        ResultatParsing resultatParsing) {

        List<Pair<String, String>> listEventParams = new ArrayList<>();

        for (int i = 0; i < listParamsEventsFromFile.getLength(); i++) {

            Node node = listParamsEventsFromFile.item(i);

            // Récupération des éléments du event actuel
            Optional<String> eventParamTypeOp = getEventParamTypeFromFile(xPath, node, resultatParsing);
            String eventParamValue = getEventParamValueFromFile(node);

            // Si on a aucune erreur dans le fichier les informations d'instanciation du event courant est
            // ajouté au résultat du parsing
            if (eventParamTypeOp.isPresent()) {
                Pair<String, String> param = new Pair<>(eventParamTypeOp.get(), eventParamValue);
                listEventParams.add(param);
            }

        }

        return listEventParams;

    }

    /**
     * Permet la récupération de la valeur du type du paramètre du event fournit en entrée.
     *
     * @param xPath           le xPath
     * @param node            le noeud corespondant à un event du fichier XML de configuration
     * @param resultatParsing le résultat de parsing qui sera mit à jour au cours du traitement en cas d'erreur de parsing
     * @return le type du paramètre du event fournit en entrée
     */
    protected Optional<String> getEventParamTypeFromFile(XPath xPath, Node node, ResultatParsing resultatParsing) {
        Optional<String> eventParamTypeOp = Optional.empty();
        try {
            String eventParamType = (String) xPath.evaluate("@" + xmlNodeParamAttrType, node, XPathConstants.STRING);
            if (eventParamType.equals("")) {
                resultatParsing.addParsingErrorType(parsingErrorType_pluralEventInvalidType);
            } else {
                eventParamTypeOp = Optional.of(eventParamType);
            }
        } catch (XPathExpressionException e) {
            resultatParsing.addParsingErrorType(parsingErrorType_pluralEventInvalidType);
        }
        return eventParamTypeOp;
    }

    /**
     * Permet la récupération de la valeur du paramètre du event fournit en entrée.
     *
     * @param node le noeud corespondant à un event du fichier XML de configuration
     * @return la valeur du paramètre du event fournit en entrée
     */
    protected String getEventParamValueFromFile(Node node) {
        String eventParamValue = node.getFirstChild().getNodeValue();
        return eventParamValue;
    }

    /**
     * Indique si le event est enabled ou non. Si l'attribut n'est pas présent, l'event est
     * considéré comme actif.
     *
     * @param xPath le XPath
     * @param node  le noeud dans le fichier correspondant à l'event
     * @return <code>true</code> si l'event est activé et <code>false</code> dans le cas contraire, dans ce cas, le
     * {@link ResultatParsing} n'est pas mis à jour
     */
    protected static boolean isEnabledEvent(XPath xPath, Node node) {
        boolean enabled = false;
        try {
            String primitiveEventEnabled = (String) xPath.evaluate("@" + XMLFileStructure.EVENT_ATTR_ENABLED.getLabel(), node, XPathConstants.STRING);
            if (primitiveEventEnabled.equals("true") || primitiveEventEnabled.equals("")) {
                enabled = true;
            }
        } catch (XPathExpressionException e) {
            // L'attribut n'est pas présent on considère que le event est à activer
            enabled = true;
        }
        return enabled;
    }

    /**
     * Permet l'enregistrement des informations extrait du fichier de configuration concernant l'event au {@link ResultatParsing}.
     *
     * @param eventName       le nom de l'event
     * @param eventType       le type de l'event
     * @param pairs           un tuple contenant le type du paramètre suivit de sa valeur
     * @param resultatParsing l'objet résultat du parsing qui sera mit à jour
     */
    abstract void addEventData(String eventName, String eventType, List<Pair<String, String>> pairs, ResultatParsing resultatParsing);

    /**
     * Méthode vérifiant qu'il n'existe pas d'event (simple ou complexe) présentant un nom déjà connu dans le résultat du parsing.
     *
     * @param name            le nom de l'event concerné
     * @param resultatParsing l'objet résultat du parsing qui sera mit à jour
     * @return vrai s'il existe un event dans le resultat du parsing présentant le nom fournit et false dans le cas contraire.
     */
    abstract boolean existingEventWithNameInResultatParsing(String name, ResultatParsing resultatParsing);

}
