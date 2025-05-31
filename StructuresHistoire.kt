package com.votregame.moteurtextuel // Adapte le package à ton projet

// Assurez-vous que VisaStatut est accessible (défini dans Joueur.kt ou un fichier partagé)
// import com.votregame.moteurtextuel.VisaStatut // Si dans un autre fichier

// --- Conditions ---

/**
 * Opérateurs pour les comparaisons numériques et autres évaluations.
 */
enum class OperateurComparaison {
    EGAL, DIFFERENT,
    SUPERIEUR, SUPERIEUR_OU_EGAL,
    INFERIEUR, INFERIEUR_OU_EGAL,
    CONTIENT, NE_CONTIENT_PAS // Pour vérifier la présence dans des listes/sets (ex: inventaire)
}

/**
 * Représente une condition à évaluer par le moteur de jeu.
 * Permet de définir des logiques complexes pour l'affichage des choix,
 * l'application des conséquences, ou le déclenchement d'événements.
 * Une condition non spécifiée (null) est généralement interprétée comme toujours vraie.
 */
sealed class Condition {
    /** Aucune condition spécifique, toujours évaluée à vrai. */
    object Aucun : Condition()

    // Conditions atomiques basées sur l'état du joueur
    data class StatNumerique(val nomStat: String, val operateur: OperateurComparaison, val valeurAttendue: Int) : Condition()
    data class Drapeau(val nomDrapeau: String, val doitEtrePresent: Boolean = true) : Condition() // Si !doitEtrePresent, vérifie l'absence
    data class ObjetInventaire(val nomObjet: String, val doitEtrePresent: Boolean = true) : Condition()
    data class StatutVisaFiancee(val statutAttendu: VisaStatut, val doitEtreEgal: Boolean = true) : Condition()
    data class ProfessionActuelleJoueur(val professionAttendue: String, val doitEtreEgale: Boolean = true) : Condition()
    data class NomPartiPolitiqueJoueur(val nomAttendu: String?, val doitEtreEgal: Boolean = true) : Condition() // nomAttendu peut être null pour vérifier l'absence de parti

    // Conditions logiques pour combiner d'autres conditions
    data class Et(val conditions: List<Condition>) : Condition()
    data class Ou(val conditions: List<Condition>) : Condition()
    data class Non(val conditionANier: Condition) : Condition()

    // TODO pour le futur:
    //  - Condition basée sur le temps écoulé dans le jeu.
    //  - Condition basée sur le nombre de visites d'un noeud.
    //  - Condition comparant deux stats du joueur entre elles.
    //  - Condition sur une variable locale au noeud.
}

// --- Conséquences ---

/**
 * Énumération des différents types de conséquences possibles qu'un choix ou un événement peut déclencher.
 */
enum class TypeConsequence {
    /** Modifie une statistique numérique du joueur. `nomParametre`=nomStat, `valeurParametre`=changement (ex: "10", "-5"). */
    MODIFIER_STAT_NUMERIQUE,
    /** Ajoute un drapeau unique à l'état du joueur. `nomParametre`=nomDrapeau. */
    AJOUTER_DRAPEAU_UNIQUE,
    /** Retire un drapeau unique de l'état du joueur. `nomParametre`=nomDrapeau. */
    RETIRER_DRAPEAU_UNIQUE,
    /** Ajoute un objet à l'inventaire du joueur. `nomParametre`=nomObjet. */
    AJOUTER_OBJET_INVENTAIRE,
    /** Retire un objet de l'inventaire du joueur. `nomParametre`=nomObjet. */
    RETIRER_OBJET_INVENTAIRE,
    /** Change le statut du visa de la fiancée. `nomParametre`="fianceeVisaStatut", `valeurParametre`=valeur de VisaStatut (ex: "REFUSE"). */
    CHANGER_STATUT_VISA_FIANCEE,
    /** Change la profession actuelle du joueur. `nomParametre`="professionActuelle", `valeurParametre`=nouvelleProfession. */
    CHANGER_PROFESSION_JOUEUR,
    /** Change le nom du parti politique du joueur. `nomParametre`="partiPolitiqueNom", `valeurParametre`=nouveauNom (ou "null" pour effacer). */
    CHANGER_NOM_PARTI_POLITIQUE,
    /** Force le passage à un autre noeud. `nomParametre`=idNoeudDestination. Prioritaire sur `ChoixHistoire.idNoeudSuivant`. */
    ALLER_AU_NOEUD,
    /** Suggère un changement de musique ou d'ambiance sonore. `nomParametre`=idMusique/Ambiance. */
    CHANGER_MUSIQUE_AMBIANCE,
    /** Ne fait explicitement rien. Utile pour des branches narratives sans effet immédiat. */
    AUCUNE
    // Potentiel pour le futur : EXECUTER_SCRIPT_COMPLEXE (nomParametre=idScript)
}

/**
 * Représente une conséquence spécifique d'un choix ou d'un événement.
 *
 * @property typeDeConsequence Le type d'effet à appliquer.
 * @property nomParametre Le nom de la cible de la conséquence (nom de stat, drapeau, objet, etc.).
 * Dépend du `typeDeConsequence`. Peut être null si non pertinent (ex: pour AUCUNE).
 * @property valeurParametre La valeur associée à la conséquence (montant, nom, ID, etc.).
 * Dépend du `typeDeConsequence`. Peut être null si non pertinent.
 * Les formats attendus sont décrits dans l'enum `TypeConsequence`.
 * @property condition Une [Condition] qui doit être remplie pour que cette conséquence s'applique.
 * Par défaut, [Condition.Aucun] (s'applique toujours).
 */
data class Consequence(
    val typeDeConsequence: TypeConsequence,
    val nomParametre: String? = null,
    val valeurParametre: String? = null,
    val condition: Condition = Condition.Aucun
)

// --- Choix et Noeuds ---

/**
 * Énumération des différents types de noeuds pour un traitement ou une signification spécifique.
 */
enum class TypeNoeud {
    STANDARD,               // Noeud narratif ou d'action classique.
    DIALOGUE,               // Principalement axé sur des échanges de paroles.
    CARREFOUR,              // Sert de point de branchement majeur.
    EVENEMENT_MAJEUR,       // Marque un tournant important dans l'histoire.
    DECOUVERTE_INFO,        // Noeud où le joueur obtient une information cruciale.
    RESOLUTION_CONFLIT,     // Noeud qui conclut une phase de tension ou un défi.
    FIN_DE_JEU_POSITIVE,    // Marque une fin de jeu favorable.
    FIN_DE_JEU_NEGATIVE,    // Marque une fin de jeu défavorable.
    FIN_DE_JEU_NEUTRE,      // Marque une fin de jeu mitigée ou ouverte.
    POINT_DE_NON_RETOUR     // Un noeud après lequel certaines branches de l'histoire deviennent inaccessibles.
}

/**
 * Représente un choix que le joueur peut faire au sein d'un [NoeudHistoire].
 *
 * @property texteAffichage Le texte du choix tel qu'il est présenté au joueur.
 * @property idNoeudSuivant L'ID du [NoeudHistoire] vers lequel ce choix mène par défaut.
 * Peut être supplanté par une [Consequence] de type [TypeConsequence.ALLER_AU_NOEUD].
 * @property consequences La liste des [Consequence] qui se déclenchent si ce choix est fait.
 * @property conditionAffichage La [Condition] requise pour que ce choix soit visible et sélectionnable par le joueur.
 * Par défaut, [Condition.Aucun] (toujours visible).
 * @property estChoixFinalIndicateur Booléen indiquant si ce choix est conçu pour mener à une conclusion (de branche ou de jeu).
 * Utile pour l'interface ou la logique de fin de jeu.
 * @property tooltipDescription Texte supplémentaire optionnel qui pourrait s'afficher au survol ou via un bouton d'aide,
 * donnant un indice ou une clarification sur le choix sans le révéler complètement.
 */
data class ChoixHistoire(
    val texteAffichage: String,
    val idNoeudSuivant: String,
    val consequences: List<Consequence> = emptyList(),
    val conditionAffichage: Condition = Condition.Aucun,
    val estChoixFinalIndicateur: Boolean = false,
    val tooltipDescription: String? = null
)

/**
 * Représente un "moment", une "scène" ou un "état" dans l'histoire du jeu.
 *
 * @property id Identifiant unique et immuable du noeud.
 * @property titre Un titre court et descriptif pour le noeud, utile pour l'organisation et le débogage.
 * @property description Le texte principal décrivant la situation, l'environnement, ou les dialogues.
 * Peut contenir des placeholders (ex: "{joueur.nom}", "{joueur.statsNumeriques.moral}")
 * qui seront remplacés par le moteur de jeu avec les valeurs actuelles.
 * @property choix La liste des [ChoixHistoire] offerts au joueur depuis ce noeud.
 * @property consequencesEntree Liste des [Consequence] qui se déclenchent automatiquement
 * lorsque le joueur entre dans ce noeud (avant l'affichage de la description et des choix).
 * @property consequencesSortie Liste des [Consequence] qui se déclenchent automatiquement
 * lorsque le joueur quitte ce noeud (après avoir fait un choix, mais avant de passer au noeud suivant).
 * @property typeDeNoeud Le [TypeNoeud] qui catégorise ce noeud, permettant un traitement ou une présentation spécifique.
 * @property tags Un ensemble de mots-clés textuels pour une catégorisation flexible (ex: "paris", "juridique", "decision_critique").
 * @property musiqueOuAmbianceId L'identifiant d'une piste musicale ou d'une ambiance sonore à jouer lorsque ce noeud est actif.
 * @property variablesLocalesAuNoeud Un espace de stockage clé-valeur (String-String pour la simplicité de sérialisation)
 * pour des données temporaires ou des états spécifiques à ce noeud
 * (ex: compteur pour une énigme, état d'un PNJ dans cette scène).
 */
data class NoeudHistoire(
    val id: String,
    val titre: String? = null,
    val description: String,
    val choix: List<ChoixHistoire> = emptyList(), // Un noeud peut ne pas avoir de choix (ex: noeud de fin)
    val consequencesEntree: List<Consequence> = emptyList(),
    val consequencesSortie: List<Consequence> = emptyList(),
    val typeDeNoeud: TypeNoeud = TypeNoeud.STANDARD,
    val tags: Set<String> = emptySet(),
    val musiqueOuAmbianceId: String? = null,
    val variablesLocalesAuNoeud: MutableMap<String, String> = mutableMapOf()
) {
    /**
     * Détermine si ce noeud représente une forme de conclusion logique de l'histoire ou d'une branche.
     * Un noeud est considéré comme une fin logique si son type est explicitement un type de fin,
     * ou s'il n'offre aucun choix (visible et menant ailleurs qu'à une fin).
     * La logique complète d'évaluation des choix visibles est du ressort du MoteurJeu.
     */
    fun estUneFinLogique(): Boolean {
        return when (typeDeNoeud) {
            TypeNoeud.FIN_DE_JEU_POSITIVE, TypeNoeud.FIN_DE_JEU_NEGATIVE, TypeNoeud.FIN_DE_JEU_NEUTRE -> true
            else -> choix.none { choixActif ->
                // Une logique plus fine ici dépendrait de l'évaluation par le moteur
                // des conditions d'affichage du choixActif et de la destination.
                // Pour la structure, on considère qu'un noeud sans choix est une fin de branche.
                // Ou si tous les choix sont marqués comme finaux.
                !choixActif.estChoixFinalIndicateur
            } && choix.isNotEmpty() // S'il y a des choix, ils doivent tous être finaux
            || choix.isEmpty() // S'il n'y a pas de choix du tout
        }
    }
}
