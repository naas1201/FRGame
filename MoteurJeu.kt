package com.votregame.moteurtextuel // Adapte le package à ton projet

// Assure-toi que les classes Joueur, NoeudHistoire, ChoixHistoire, Consequence, Condition,
// TypeConsequence, OperateurComparaison, VisaStatut, TypeNoeud sont accessibles.

/**
 * Le moteur principal du jeu textuel.
 * Gère l'état du jeu, la progression de l'histoire, l'évaluation des conditions
 * et l'application des conséquences.
 *
 * @property joueur L'instance du joueur actuel.
 */
class MoteurJeu(var joueur: Joueur) {

    private val mapNoeuds: MutableMap<String, NoeudHistoire> = mutableMapOf()
    private var idNoeudActuel: String? = null
    // Gère si les conséquences d'entrée pour le noeud actuel ont déjà été traitées dans le cycle de vie de ce noeud.
    // Réinitialisé à chaque changement effectif de idNoeudActuel.
    private var consequencesEntreeTraiteesPourNoeudActuel: Boolean = false

    private val MAX_REDIRECT_DEPTH = 10 // Pour éviter les boucles infinies de redirection

    // --- Initialisation et Chargement ---

    fun chargerHistoire(listeNoeuds: List<NoeudHistoire>) {
        mapNoeuds.clear()
        listeNoeuds.forEach { noeud ->
            if (mapNoeuds.containsKey(noeud.id)) {
                logAvertissement("L'ID de noeud '${noeud.id}' est dupliqué. Le dernier sera utilisé.")
            }
            mapNoeuds[noeud.id] = noeud
        }
        idNoeudActuel = null
        consequencesEntreeTraiteesPourNoeudActuel = false
        if (mapNoeuds.isEmpty()) {
            logAvertissement("Aucune histoire n'a été chargée.")
        } else {
            logInfo("${mapNoeuds.size} noeuds chargés.")
        }
    }

    fun commencerJeu(idNoeudInitial: String): Boolean {
        if (!mapNoeuds.containsKey(idNoeudInitial)) {
            logErreur("Noeud initial '$idNoeudInitial' non trouvé.")
            if (mapNoeuds.isNotEmpty()) {
                val premierNoeudId = mapNoeuds.keys.first()
                logInfo("Tentative de démarrage au premier noeud disponible: '$premierNoeudId'")
                return changerDeNoeudEffectif(premierNoeudId, 0)
            }
            return false
        }
        logInfo("Jeu démarré au noeud '$idNoeudInitial'.")
        return changerDeNoeudEffectif(idNoeudInitial, 0)
    }

    // --- Changement de Noeud Contrôlé (avec gestion des redirections d'entrée) ---

    private fun changerDeNoeudEffectif(nouveauIdNoeud: String, depth: Int): Boolean {
        if (depth > MAX_REDIRECT_DEPTH) {
            logErreur("Profondeur maximale de redirection atteinte ($MAX_REDIRECT_DEPTH) en essayant d'aller à '$nouveauIdNoeud'. Boucle probable.")
            this.idNoeudActuel = null // Bloquer le jeu pour éviter une boucle infinie
            return false
        }

        if (!mapNoeuds.containsKey(nouveauIdNoeud)) {
            logErreur("Tentative de changement vers un noeud inexistant: '$nouveauIdNoeud'.")
            this.idNoeudActuel = null
            return false
        }

        this.idNoeudActuel = nouveauIdNoeud
        this.consequencesEntreeTraiteesPourNoeudActuel = false // Réinitialiser pour le nouveau noeud

        // Appliquer les conséquences d'entrée et gérer les redirections potentielles
        val noeudDestination = getNoeudActuel()
        if (noeudDestination != null) {
            val (redirectId, _) = appliquerConsequencesSpecifiques(
                noeudDestination.consequencesEntree,
                "entrée au noeud ${noeudDestination.id}",
                depth
            )
            if (redirectId != null) {
                logInfo("Redirection par consequenceEntree du noeud '${noeudDestination.id}' vers '$redirectId'.")
                return changerDeNoeudEffectif(redirectId, depth + 1) // Appel récursif avec profondeur augmentée
            }
            this.consequencesEntreeTraiteesPourNoeudActuel = true // Marquer comme traitées si pas de redirection
        }
        return true
    }


    // --- Accès à l'état du jeu ---

    fun getNoeudActuel(): NoeudHistoire? = idNoeudActuel?.let { mapNoeuds[it] }

    fun getDescriptionNoeudActuelFormattee(): String {
        val noeud = getNoeudActuel() ?: return "[MOTEUR ERREUR]: Aucun noeud actif."
        return remplacerPlaceholders(noeud.description, noeud)
    }

    fun getChoixVisiblesNoeudActuel(): List<ChoixHistoire> {
        val noeud = getNoeudActuel() ?: return emptyList()
        return noeud.choix.filter { choix -> evaluerCondition(choix.conditionAffichage) }
    }

    // --- Logique de progression ---

    fun faireChoix(indexDuChoixVisible: Int): Boolean {
        val noeudCourant = getNoeudActuel()
        if (noeudCourant == null) {
            logErreur("Aucun noeud actuel. Impossible de faire un choix.")
            return false
        }

        val choixVisibles = getChoixVisiblesNoeudActuel()
        if (indexDuChoixVisible < 0 || indexDuChoixVisible >= choixVisibles.size) {
            logErreur("Index de choix visible ($indexDuChoixVisible) invalide pour le noeud '${noeudCourant.id}'.")
            return false
        }

        val choixFait = choixVisibles[indexDuChoixVisible]
        logInfo("Choix fait: '${choixFait.texteAffichage}' (cible initiale: '${choixFait.idNoeudSuivant}')")

        // 0. Appliquer les conséquences de sortie du noeud actuel
        // Une redirection ici prendra le pas sur la destination du choix.
        var (idRedirectionSortie, _) = appliquerConsequencesSpecifiques(
            noeudCourant.consequencesSortie,
            "sortie du noeud ${noeudCourant.id}",
            0 // depth initiale pour cette phase
        )
        var prochainNoeudIdCible = idRedirectionSortie ?: choixFait.idNoeudSuivant

        if (idRedirectionSortie != null) {
            logInfo("Redirection par consequenceSortie du noeud '${noeudCourant.id}' vers '$prochainNoeudIdCible'.")
        }

        // 1. Appliquer les conséquences du choix (qui peuvent aussi rediriger)
        // Une redirection ici prendra le pas sur `prochainNoeudIdCible` (qui vient de la sortie ou du choix).
        val (idRedirectionChoix, _) = appliquerConsequencesSpecifiques(
            choixFait.consequences,
            "choix '${choixFait.texteAffichage}'",
            0 // depth initiale pour cette phase
        )
        if (idRedirectionChoix != null) {
            prochainNoeudIdCible = idRedirectionChoix
            logInfo("Redirection par consequence du choix '${choixFait.texteAffichage}' vers '$prochainNoeudIdCible'.")
        }
        
        // Cas spécial: si une conséquence du choix était ALLER_AU_NOEUD mais n'a pas été la "dernière" à s'appliquer
        // (car appliquerConsequencesSpecifiques retourne le premier redirectId non-null),
        // on vérifie si la logique originale de `lastOrNull` pour `ALLER_AU_NOEUD` dans le choix doit s'appliquer.
        // Cette logique est maintenant intégrée dans `appliquerConsequencesSpecifiques` qui retourne le premier `ALLER_AU_NOEUD` valide.

        return changerDeNoeudEffectif(prochainNoeudIdCible, 0)
    }

    // --- Évaluation des Conditions ---

    fun evaluerCondition(condition: Condition): Boolean {
        return when (condition) {
            is Condition.Aucun -> true
            is Condition.StatNumerique -> {
                val statActuelle = joueur.getStatNumerique(condition.nomStat)
                when (condition.operateur) {
                    OperateurComparaison.EGAL -> statActuelle == condition.valeurAttendue
                    OperateurComparaison.DIFFERENT -> statActuelle != condition.valeurAttendue
                    OperateurComparaison.SUPERIEUR -> statActuelle > condition.valeurAttendue
                    OperateurComparaison.SUPERIEUR_OU_EGAL -> statActuelle >= condition.valeurAttendue
                    OperateurComparaison.INFERIEUR -> statActuelle < condition.valeurAttendue
                    OperateurComparaison.INFERIEUR_OU_EGAL -> statActuelle <= condition.valeurAttendue
                    OperateurComparaison.CONTIENT, OperateurComparaison.NE_CONTIENT_PAS -> {
                        logAvertissement("Opérateur ${condition.operateur} non pertinent pour Condition.StatNumerique simple. Évalué à faux.")
                        false
                    }
                }
            }
            is Condition.Drapeau -> joueur.aLeDrapeau(condition.nomDrapeau) == condition.doitEtrePresent
            is Condition.ObjetInventaire -> joueur.possedeObjet(condition.nomObjet) == condition.doitEtrePresent
            is Condition.StatutVisaFiancee -> (joueur.fianceeVisaStatut == condition.statutAttendu) == condition.doitEtreEgal
            is Condition.ProfessionActuelleJoueur -> (joueur.professionActuelle == condition.professionAttendue) == condition.doitEtreEgale
            is Condition.NomPartiPolitiqueJoueur -> (joueur.partiPolitiqueNom == condition.nomAttendu) == condition.doitEtreEgal
            is Condition.Et -> condition.conditions.all { evaluerCondition(it) }
            is Condition.Ou -> condition.conditions.any { evaluerCondition(it) }
            is Condition.Non -> !evaluerCondition(condition.conditionANier)
        }
    }

    // --- Application des Conséquences ---

    /**
     * Applique une liste de conséquences.
     * @return Pair<String?, Boolean> où String? est l'ID du noeud de redirection si ALLER_AU_NOEUD est déclenché,
     * et Boolean indique si au moins une conséquence a été effectivement appliquée.
     */
    private fun appliquerConsequencesSpecifiques(
        consequences: List<Consequence>,
        contexteDebug: String,
        currentRedirectDepth: Int
    ): Pair<String?, Boolean> {
        if (consequences.isEmpty()) return Pair(null, false)

        logInfo("Application des conséquences pour contexte: [$contexteDebug]")
        var redirectId: String? = null
        var consequenceAppliquee = false

        for ((index, consequence) in consequences.withIndex()) {
            if (evaluerCondition(consequence.condition)) {
                logDebug("  - Cons. #${index + 1} (${consequence.typeDeConsequence}) condition remplie.")
                val (effetRedirectId) = appliquerUneConsequence(consequence, currentRedirectDepth)
                consequenceAppliquee = true
                if (effetRedirectId != null && redirectId == null) { // Prend la première redirection rencontrée
                    redirectId = effetRedirectId
                    logInfo("    -> Redirection prioritaire par cette conséquence vers '$redirectId'.")
                    // On pourrait choisir de stopper ici ou de continuer les autres conséquences non-redirectrices.
                    // Pour l'instant, on continue, mais la première redirection prendra effet.
                }
            } else {
                logDebug("  - Cons. #${index + 1} (${consequence.typeDeConsequence}) non appliquée (condition non remplie).")
            }
        }
        return Pair(redirectId, consequenceAppliquee)
    }

    /**
     * Applique une conséquence unique.
     * @return Pair<String?> où String? est l'ID du noeud de redirection si ALLER_AU_NOEUD est déclenché.
     */
    private fun appliquerUneConsequence(consequence: Consequence, currentRedirectDepth: Int): Pair<String?, Unit> {
        val nomParam = consequence.nomParametre
        val valParam = consequence.valeurParametre
        var redirectId: String? = null

        when (consequence.typeDeConsequence) {
            TypeConsequence.MODIFIER_STAT_NUMERIQUE -> {
                if (nomParam != null && valParam != null) {
                    val valeurChange = valParam.toIntOrNull()
                    if (valeurChange != null) {
                        joueur.modifierStatNumerique(nomParam, valeurChange)
                    } else {
                        logAvertissement("MODIFIER_STAT_NUMERIQUE: valeurParam '$valParam' invalide pour $nomParam.")
                    }
                } else {
                    logAvertissement("MODIFIER_STAT_NUMERIQUE: nomParam ou valParam manquant.")
                }
            }
            TypeConsequence.AJOUTER_DRAPEAU_UNIQUE -> nomParam?.let { joueur.ajouterDrapeauUnique(it) }
            TypeConsequence.RETIRER_DRAPEAU_UNIQUE -> nomParam?.let {
                if (joueur.statsUniques.remove(it)) logDebug("    Drapeau '$it' retiré.")
                else logDebug("    Tentative de retrait du drapeau '$it', mais non trouvé.")
            }
            TypeConsequence.AJOUTER_OBJET_INVENTAIRE -> nomParam?.let { joueur.ajouterObjetInventaire(it) }
            TypeConsequence.RETIRER_OBJET_INVENTAIRE -> nomParam?.let { joueur.retirerObjetInventaire(it) }
            TypeConsequence.CHANGER_STATUT_VISA_FIANCEE -> {
                try {
                    valParam?.let {
                        val nouveauStatut = VisaStatut.valueOf(it.uppercase())
                        joueur.fianceeVisaStatut = nouveauStatut
                        logDebug("    Statut Visa Fiancée changé à $nouveauStatut.")
                    } ?: logAvertissement("CHANGER_STATUT_VISA_FIANCEE: valParam manquant.")
                } catch (e: IllegalArgumentException) {
                    logErreur("Valeur de VisaStatut invalide pour CHANGER_STATUT_VISA_FIANCEE: $valParam")
                }
            }
            TypeConsequence.CHANGER_PROFESSION_JOUEUR -> {
                valParam?.let {
                    joueur.professionActuelle = it
                    logDebug("    Profession joueur changée à '$it'.")
                } ?: logAvertissement("CHANGER_PROFESSION_JOUEUR: valParam manquant.")
            }
            TypeConsequence.CHANGER_NOM_PARTI_POLITIQUE -> {
                val nouveauNom = if (valParam.equals("null", ignoreCase = true) || valParam == null) null else valParam
                joueur.partiPolitiqueNom = nouveauNom
                logDebug("    Nom du parti politique changé à '${nouveauNom ?: "Aucun"}'.")
            }
            TypeConsequence.ALLER_AU_NOEUD -> {
                if (nomParam != null) {
                    if (currentRedirectDepth < MAX_REDIRECT_DEPTH) {
                        redirectId = nomParam
                        // La redirection effective sera gérée par la fonction appelante
                    } else {
                        logErreur("Profondeur de redirection MAX atteinte. ALLER_AU_NOEUD vers '$nomParam' ignoré.")
                    }
                } else {
                    logAvertissement("ALLER_AU_NOEUD: nomParam (ID du noeud destination) manquant.")
                }
            }
            TypeConsequence.CHANGER_MUSIQUE_AMBIANCE -> {
                logInfo("Demande de changement de musique/ambiance vers: ${nomParam ?: "non spécifié"}")
                // TODO: Envoyer un événement ou appeler un callback pour le système audio.
            }
            TypeConsequence.AUCUNE -> { /* Ne rien faire explicitement */ }
        }
        return Pair(redirectId, Unit)
    }

    // --- Utilitaires ---

    fun remplacerPlaceholders(texte: String, noeudContexte: NoeudHistoire?): String {
        var resultat = texte
        val placeholderRegex = """\{([\w.:]+)\}""".toRegex() // Supporte maintenant les : pour les arguments

        placeholderRegex.findAll(texte).forEach { matchResult ->
            val placeholderComplet = matchResult.value
            val cheminEtArgs = matchResult.groupValues[1]
            val partsCheminArgs = cheminEtArgs.split(':', limit = 2)
            val cheminPlaceholder = partsCheminArgs[0]
            val argsPlaceholder = if (partsCheminArgs.size > 1) partsCheminArgs[1] else null

            val parts = cheminPlaceholder.split('.')
            var valeurRemplacement = "[PLACEHOLDER_INCONNU: $cheminPlaceholder]"

            if (parts.isNotEmpty()) {
                when (parts[0].lowercase()) {
                    "joueur" -> if (parts.size > 1) valeurRemplacement = getValeurJoueurParChemin(parts.drop(1), argsPlaceholder)
                    "noeud" -> if (parts.size > 1 && noeudContexte != null) valeurRemplacement = getValeurNoeudParChemin(noeudContexte, parts.drop(1), argsPlaceholder)
                    "jeu" -> if (parts.size > 1) valeurRemplacement = getValeurJeuParChemin(parts.drop(1), argsPlaceholder)
                }
            }
            resultat = resultat.replace(placeholderComplet, valeurRemplacement)
        }
        return resultat
    }

    private fun getValeurJoueurParChemin(parts: List<String>, args: String?): String {
        if (parts.isEmpty()) return "[CHEMIN_JOUEUR_VIDE]"
        return when (parts[0].lowercase()) {
            "nom" -> joueur.nom
            "professionactuelle" -> joueur.professionActuelle
            "fianceenom" -> joueur.fianceeNom
            "fianceevisastatut" -> joueur.fianceeVisaStatut.name
            "partipolitiquenom" -> joueur.partiPolitiqueNom ?: "Aucun"
            "statsnumeriques" -> if (parts.size > 1) joueur.getStatNumerique(parts[1]).toString() else "[STAT_MANQUANTE]"
            "inventaire" -> when {
                parts.size > 1 && parts[1].equals("contient", ignoreCase = true) && parts.size > 2 -> joueur.possedeObjet(parts[2]).toString()
                else -> joueur.inventaire.joinToString(", ")
            }
            "statsuniques" -> when {
                 parts.size > 1 && parts[1].equals("contient", ignoreCase = true) && parts.size > 2 -> joueur.aLeDrapeau(parts[2]).toString()
                 else -> joueur.statsUniques.joinToString(", ")
            }
            // Ajouter d'autres propriétés directes du joueur ici
            else -> "[PROPRIETE_JOUEUR_INCONNUE: ${parts.joinToString(".")}]"
        }
    }

    private fun getValeurNoeudParChemin(noeud: NoeudHistoire, parts: List<String>, args: String?): String {
        if (parts.isEmpty()) return "[CHEMIN_NOEUD_VIDE]"
        return when (parts[0].lowercase()) {
            "id" -> noeud.id
            "titre" -> noeud.titre ?: ""
            "tags" -> if (parts.size > 1 && parts[1].equals("contient", ignoreCase = true) && parts.size > 2) noeud.tags.contains(parts[2]).toString() else noeud.tags.joinToString(", ")
            "variableslocales" -> if (parts.size > 1) noeud.variablesLocalesAuNoeud[parts[1]] ?: "[VAR_LOCALE_INCONNUE: ${parts[1]}]" else "[CLE_VAR_LOCALE_MANQUANTE]"
            "typedeNoeud" -> noeud.typeDeNoeud.name
            else -> "[PROPRIETE_NOEUD_INCONNUE: ${parts.joinToString(".")}]"
        }
    }

    private fun getValeurJeuParChemin(parts: List<String>, args: String?): String {
        if (parts.isEmpty()) return "[CHEMIN_JEU_VIDE]"
        return when (parts[0].lowercase()) {
            "noeudactuelid" -> idNoeudActuel ?: "Aucun"
            // "tempstotalecoule" -> // Logique de temps de jeu à implémenter
            // "nombretours" -> // Logique de comptage de tours
            else -> "[PROPRIETE_JEU_INCONNUE: ${parts.joinToString(".")}]"
        }
    }


    fun setVariableLocaleNoeudActuel(cle: String, valeur: String) {
        getNoeudActuel()?.variablesLocalesAuNoeud?.set(cle, valeur)
        logDebug("Variable locale noeud ['${getNoeudActuel()?.id}']['$cle'] mise à '$valeur'.")
    }

    fun getVariableLocaleNoeudActuel(cle: String): String? {
        return getNoeudActuel()?.variablesLocalesAuNoeud?.get(cle)
    }

    // --- Logging ---
    private fun logInfo(message: String) = println("[MOTEUR INFO] $message")
    private fun logAvertissement(message: String) = println("[MOTEUR AVERT] $message")
    private fun logErreur(message: String) = println("[MOTEUR ERREUR] $message")
    private fun logDebug(message: String) = println("[MOTEUR DEBUG] $message") // Pourrait être conditionnel (ex: si mode debug activé)

    // --- Fonctions à considérer pour l'avenir (non implémentées) ---
    // fun sauvegarderPartie(nomFichier: String) { /* Logique de sérialisation de l'état du joueur et de idNoeudActuel, variablesLocalesAuNoeud, etc. */ }
    // fun chargerPartie(nomFichier: String): Boolean { /* Logique de désérialisation */ return false }
    // fun enregistrerEvenementCallback(typeEvenement: String, callback: () -> Unit) { /* Pour un système d'événements découplé */ }
}
