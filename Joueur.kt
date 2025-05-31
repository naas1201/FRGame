package com.votregame.moteurtextuel // Adapte le package à ton projet

/**
 * Représente le joueur et toutes ses caractéristiques.
 *
 * @property nom Le nom du personnage joueur.
 * @property professionActuelle La profession actuelle du joueur (évolue dans le jeu).
 * @property fianceeNom Le nom de sa fiancée.
 * @property fianceeVisaStatut L'état actuel de la demande de visa de sa fiancée.
 * @property partiPolitiqueNom Le nom du parti politique si créé.
 * @property statsUniques Un ensemble de booléens pour marquer des événements uniques ou des états.
 * @property inventaire Liste des objets ou documents importants possédés.
 * @property statsNumeriques Un dictionnaire flexible pour toutes les statistiques numériques
 * qui peuvent évoluer (compétences, ressources, états émotionnels, relations, etc.).
 * Exemples de clés :
 * - "argent": Les ressources financières.
 * - "connaissanceJuridique": Niveau de compréhension du droit.
 * - "eloquence": Capacité à persuader.
 * - "strategiePolitique": Compétence en manœuvres politiques.
 * - "reseauContacts": Étendue et influence de son réseau.
 * - "soutienPopulaire": Niveau de soutien de l'opinion publique.
 * - "moral": État d'esprit général, motivation.
 * - "stress": Niveau de pression ressenti.
 * - "determination": Force de volonté pour atteindre ses objectifs.
 * - "colereJustifiee": Moteur lié à l'injustice subie.
 * - "influencePolitique": Capacité à impacter les décisions politiques.
 * - "santeMentale": Stabilité psychologique face aux épreuves.
 * - "tempsDisponible": Ressource abstraite pour les actions (ex: points d'action par jour).
 *
 * NOUVELLES VARIABLES POUR PLUS DE PROFONDEUR ET DE SÉRIEUX:
 * - "reputationPublique": Perception du joueur par le public et les médias (peut être négative).
 * - "integriteMorale": Constance dans les choix éthiques, résistance à la corruption.
 * - "relationFiancee": Qualité et solidité de la relation avec sa fiancée.
 * - "credibilitePolitique": Degré de sérieux accordé par les autres acteurs politiques.
 * - "fatiguePhysique": Niveau d'épuisement physique dû aux efforts.
 * - "competenceMediatique": Aisance et efficacité face aux médias.
 * - "loyauteEquipe": Niveau de confiance et de dévouement de son équipe politique (si formée).
 * - "risqueJuridique": Exposition à des problèmes légaux (diffamation, financement, etc.).
 * - "cultureGeneralePolitique": Compréhension des idéologies, histoire politique.
 * - "connaissanceInstitutions": Maîtrise du fonctionnement réel des institutions françaises.
 * - "patience": Capacité à gérer les frustrations et les délais.
 * - "discernement": Capacité à juger sainement les situations et les personnes.
 */
data class Joueur(
    val nom: String = "Jean Dupont", // Nom par défaut, peut être personnalisable
    var professionActuelle: String = "Consultant (sans emploi)",
    val fianceeNom: String = "Maria Dela Cruz", // Nom de la fiancée
    var fianceeVisaStatut: VisaStatut = VisaStatut.EN_ATTENTE,
    var partiPolitiqueNom: String? = null, // Initialement nul, devient "France Raison"
    val statsUniques: MutableSet<String> = mutableSetOf(), // Ex: "VISA_REFUSE", "PARTI_LANCE"
    val inventaire: MutableList<String> = mutableListOf(), // Ex: "Dossier Visa", "Statuts du Parti"
    val statsNumeriques: MutableMap<String, Int> = mutableMapOf(
        // Stats initiales
        "argent" to 5000,
        "connaissanceJuridique" to 5,
        "eloquence" to 30,
        "strategiePolitique" to 10,
        "reseauContacts" to 20,
        "soutienPopulaire" to 0,
        "moral" to 40,
        "stress" to 60,
        "determination" to 30,
        "colereJustifiee" to 0,
        "influencePolitique" to 0,
        "santeMentale" to 70,
        "tempsDisponible" to 10,

        // Nouvelles stats pour profondeur et sérieux
        "reputationPublique" to 50, // Neutre au départ
        "integriteMorale" to 60,    // Commence avec une bonne intégrité
        "relationFiancee" to 80,   // Relation solide au début
        "credibilitePolitique" to 10, // Très faible au début
        "fatiguePhysique" to 10,     // Peu fatigué au début (0 = pas fatigué, 100 = épuisé)
        "competenceMediatique" to 15,// Faible au début
        "loyauteEquipe" to 0,        // Pas d'équipe au début
        "risqueJuridique" to 5,      // Faible risque initial
        "cultureGeneralePolitique" to 25,
        "connaissanceInstitutions" to 10,
        "patience" to 50,
        "discernement" to 40
    )
) {

    /**
     * Modifie une statistique numérique.
     * @param nomStat Le nom de la statistique à modifier (clé dans `statsNumeriques`).
     * @param valeur Le montant à ajouter (peut être négatif pour diminuer).
     * @param min La valeur minimale autorisée pour cette stat (par défaut 0).
     * @param max La valeur maximale autorisée pour cette stat (par défaut 100).
     */
    fun modifierStatNumerique(nomStat: String, valeur: Int, min: Int = 0, max: Int = 100) {
        val valeurActuelle = statsNumeriques[nomStat] ?: 0
        var nouvelleValeur = valeurActuelle + valeur

        // Gérer les cas spécifiques pour min/max
        // Certaines stats comme 'argent' ou 'tempsDisponible' peuvent ne pas avoir de max à 100
        // 'reputationPublique' peut devenir négative.
        val effectiveMax = when (nomStat) {
            "argent", "tempsDisponible" -> Int.MAX_VALUE
            else -> max
        }
        val effectiveMin = when (nomStat) {
            "argent" -> Int.MIN_VALUE // Peut être endetté
            "reputationPublique" -> -100 // Peut avoir une très mauvaise réputation
            else -> min
        }

        nouvelleValeur = nouvelleValeur.coerceIn(effectiveMin, effectiveMax)
        statsNumeriques[nomStat] = nouvelleValeur
        // Log pour le débogage, peut être retiré ou géré par l'UI plus tard
        println("DEBUG: Statistique Joueur '${nomStat}' mise à jour de $valeurActuelle à $nouvelleValeur (valeur ajoutée: $valeur)")
    }

    /**
     * Récupère la valeur d'une statistique numérique.
     * @param nomStat Le nom de la statistique.
     * @return La valeur de la statistique, ou 0 si elle n'existe pas.
     */
    fun getStatNumerique(nomStat: String): Int {
        return statsNumeriques[nomStat] ?: 0
    }

    /**
     * Ajoute un drapeau unique pour marquer un événement.
     * @param drapeau Le nom du drapeau (ex: "VISA_REFUSE_FIANCEE").
     */
    fun ajouterDrapeauUnique(drapeau: String) {
        statsUniques.add(drapeau)
        println("DEBUG: Drapeau Joueur ajouté: '$drapeau'")
    }

    /**
     * Vérifie si un drapeau unique a été activé.
     * @param drapeau Le nom du drapeau à vérifier.
     * @return True si le drapeau est présent, false sinon.
     */
    fun aLeDrapeau(drapeau: String): Boolean {
        return statsUniques.contains(drapeau)
    }

    /**
     * Ajoute un objet à l'inventaire.
     */
    fun ajouterObjetInventaire(objet: String) {
        inventaire.add(objet)
        println("DEBUG: Objet ajouté à l'inventaire: '$objet'")
    }

    /**
     * Retire un objet de l'inventaire.
     * @return True si l'objet a été trouvé et retiré, false sinon.
     */
    fun retirerObjetInventaire(objet: String): Boolean {
        val removed = inventaire.remove(objet)
        if (removed) {
            println("DEBUG: Objet retiré de l'inventaire: '$objet'")
        } else {
            println("DEBUG: Tentative de retrait de l'objet '$objet', mais non trouvé dans l'inventaire.")
        }
        return removed
    }

    /**
     * Vérifie si le joueur possède un objet spécifique.
     */
    fun possedeObjet(objet: String): Boolean {
        return inventaire.contains(objet)
    }

    override fun toString(): String {
        // Pour une meilleure lisibilité, on pourrait choisir quelles stats afficher ici,
        // mais pour l'instant, afficher toutes les statsNumeriques est complet pour le débogage.
        val statsNumeriquesFormattees = statsNumeriques.entries.joinToString("\n    ") { "${it.key}: ${it.value}" }
        return """
            Joueur: $nom
            Profession: $professionActuelle
            Fiancée: $fianceeNom (Visa: $fianceeVisaStatut)
            Parti Politique: ${partiPolitiqueNom ?: "Aucun"}
            Stats Uniques: $statsUniques
            Inventaire: $inventaire
            Stats Numériques:
                $statsNumeriquesFormattees
        """.trimIndent()
    }
}

/**
 * Énumération pour le statut du visa de la fiancée.
 * Cela rend le code plus lisible et moins sujet aux erreurs que d'utiliser des chaînes de caractères libres.
 */
enum class VisaStatut {
    NON_DEMANDE,
    EN_ATTENTE,
    REFUSE,
    APPROUVE,
    APPEL_EN_COURS // Ajouté pour plus de réalisme dans le processus légal
}

// Exemple d'utilisation (pour tester rapidement, peut être mis dans une fonction main ou un test)
/*
fun main() {
    val joueur = Joueur()
    println("--- ÉTAT INITIAL DU JOUEUR ---")
    println(joueur)

    joueur.modifierStatNumerique("argent", -2000)
    joueur.fianceeVisaStatut = VisaStatut.REFUSE
    joueur.ajouterDrapeauUnique("FIANCEE_VISA_REFUSE")
    joueur.modifierStatNumerique("colereJustifiee", 50, 0, 100)
    joueur.modifierStatNumerique("determination", 30, 0, 100)
    joueur.professionActuelle = "Apprenti Juriste"
    joueur.ajouterObjetInventaire("Code Civil (Annoté)")
    joueur.modifierStatNumerique("connaissanceJuridique", 20, 0, 100)
    joueur.modifierStatNumerique("fatiguePhysique", 15, 0, 100) // Etudier fatigue
    joueur.modifierStatNumerique("relationFiancee", -10, 0, 100) // Le stress affecte la relation

    println("\n--- APRÈS REFUS VISA ET DÉBUT ÉTUDES DE DROIT ---")
    println(joueur)

    joueur.partiPolitiqueNom = "France Raison"
    joueur.ajouterDrapeauUnique("PARTI_FRANCE_RAISON_LANCE")
    joueur.modifierStatNumerique("soutienPopulaire", 5, 0, 100)
    joueur.modifierStatNumerique("reputationPublique", 10, -100, 100) // Gagne un peu de visibilité
    joueur.modifierStatNumerique("credibilitePolitique", 5, 0, 100) // Un peu plus crédible
    joueur.modifierStatNumerique("risqueJuridique", 10, 0, 100) // Lancer un parti augmente les risques

    println("\n--- APRÈS LANCEMENT DU PARTI POLITIQUE ---")
    println(joueur)
}
*/
