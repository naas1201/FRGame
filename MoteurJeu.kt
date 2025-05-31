package com.votregame.moteurtextuel // Adapte le package à ton projet

// Import pour la sérialisation JSON avec Gson
// Assurez-vous d'ajouter la dépendance Gson à votre projet Android:
// implementation("com.google.code.gson:gson:2.9.0") // (ou version plus récente)
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Pour les types génériques comme Map

// --- Définition de la structure pour l'état de sauvegarde ---
/**
 * Classe de données représentant l'état du jeu à sauvegarder ou charger.
 */
data class EtatSauvegarde(
    val joueurSerialise: Joueur,
    val idNoeudActuelSauvegarde: String?,
    val consequencesEntreeTraiteesSauvegarde: Boolean,
    // Sauvegarde les variables locales de tous les noeuds qui en ont.
    // Clé externe: ID du noeud, Valeur: la map des variables locales de ce noeud.
    val variablesLocalesDeTousLesNoeuds: Map<String, Map<String, String>>
)

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
    private var consequencesEntreeTraiteesPourNoeudActuel: Boolean = false
    private val MAX_REDIRECT_DEPTH = 10
    private val gson = Gson() // Instance de Gson pour la sérialisation

    // --- Initialisation et Chargement ---

    fun chargerHistoire(listeNoeuds: List<NoeudHistoire>) {
        mapNoeuds.clear()
        listeNoeuds.forEach { noeud ->
            if (mapNoeuds.containsKey(noeud.id)) {
                logAvertissement("L'ID de noeud '${noeud.id}' est dupliqué. Le dernier sera utilisé.")
            }
            // Important: S'assurer que chaque noeud a sa propre instance mutable de variablesLocalesAuNoeud
            // Si les NoeudHistoire sont partagés ou recréés, cette map pourrait être partagée ou perdue.
            // Il est plus sûr de s'assurer que mapNoeuds contient des copies distinctes si nécessaire,
            // ou que la structure NoeudHistoire garantit une map mutable unique par instance.
            // Pour cet exemple, on assume que la listeNoeuds fournit des instances avec des maps distinctes.
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

    // --- Changement de Noeud Contrôlé ---

    private fun changerDeNoeudEffectif(nouveauIdNoeud: String, depth: Int): Boolean {
        if (depth > MAX_REDIRECT_DEPTH) {
            logErreur("Profondeur maximale de redirection atteinte ($MAX_REDIRECT_DEPTH) en essayant d'aller à '$nouveauIdNoeud'. Boucle probable.")
            this.idNoeudActuel = null
            return false
        }

        if (!mapNoeuds.containsKey(nouveauIdNoeud)) {
            logErreur("Tentative de changement vers un noeud inexistant: '$nouveauIdNoeud'.")
            this.idNoeudActuel = null
            return false
        }

        this.idNoeudActuel = nouveauIdNoeud
        this.consequencesEntreeTraiteesPourNoeudActuel = false

        val noeudDestination = getNoeudActuel()
        if (noeudDestination != null) {
            val (redirectId, _) = appliquerConsequencesSpecifiques(
                noeudDestination.consequencesEntree,
                "entrée au noeud ${noeudDestination.id}",
                depth
            )
            if (redirectId != null) {
                logInfo("Redirection par consequenceEntree du noeud '${noeudDestination.id}' vers '$redirectId'.")
                return changerDeNoeudEffectif(redirectId, depth + 1)
            }
            this.consequencesEntreeTraiteesPourNoeudActuel = true
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

        var (idRedirectionSortie, _) = appliquerConsequencesSpecifiques(
            noeudCourant.consequencesSortie,
            "sortie du noeud ${noeudCourant.id}",
            0
        )
        var prochainNoeudIdCible = idRedirectionSortie ?: choixFait.idNoeudSuivant

        if (idRedirectionSortie != null) {
            logInfo("Redirection par consequenceSortie du noeud '${noeudCourant.id}' vers '$prochainNoeudIdCible'.")
        }

        val (idRedirectionChoix, _) = appliquerConsequencesSpecifiques(
            choixFait.consequences,
            "choix '${choixFait.texteAffichage}'",
            0
        )
        if (idRedirectionChoix != null) {
            prochainNoeudIdCible = idRedirectionChoix
            logInfo("Redirection par consequence du choix '${choixFait.texteAffichage}' vers '$prochainNoeudIdCible'.")
        }
        
        return changerDeNoeudEffectif(prochainNoeudIdCible, 0)
    }

    // --- Évaluation des Conditions ---

    fun evaluerCondition(condition: Condition): Boolean {
        // ... (code de evaluerCondition inchangé, voir version précédente)
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

    private fun appliquerConsequencesSpecifiques(
        consequences: List<Consequence>,
        contexteDebug: String,
        currentRedirectDepth: Int
    ): Pair<String?, Boolean> {
        // ... (code de appliquerConsequencesSpecifiques inchangé, voir version précédente)
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
                }
            } else {
                logDebug("  - Cons. #${index + 1} (${consequence.typeDeConsequence}) non appliquée (condition non remplie).")
            }
        }
        return Pair(redirectId, consequenceAppliquee)
    }

    private fun appliquerUneConsequence(consequence: Consequence, currentRedirectDepth: Int): Pair<String?, Unit> {
        // ... (code de appliquerUneConsequence inchangé, voir version précédente)
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
                    } else {
                        logErreur("Profondeur de redirection MAX atteinte. ALLER_AU_NOEUD vers '$nomParam' ignoré.")
                    }
                } else {
                    logAvertissement("ALLER_AU_NOEUD: nomParam (ID du noeud destination) manquant.")
                }
            }
            TypeConsequence.CHANGER_MUSIQUE_AMBIANCE -> {
                logInfo("Demande de changement de musique/ambiance vers: ${nomParam ?: "non spécifié"}")
            }
            TypeConsequence.AUCUNE -> { /* Ne rien faire explicitement */ }
        }
        return Pair(redirectId, Unit)
    }

    // --- Utilitaires ---

    fun remplacerPlaceholders(texte: String, noeudContexte: NoeudHistoire?): String {
        // ... (code de remplacerPlaceholders inchangé, voir version précédente)
        var resultat = texte
        val placeholderRegex = """\{([\w.:]+)\}""".toRegex()

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
        // ... (code de getValeurJoueurParChemin inchangé, voir version précédente)
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
            else -> "[PROPRIETE_JOUEUR_INCONNUE: ${parts.joinToString(".")}]"
        }
    }

    private fun getValeurNoeudParChemin(noeud: NoeudHistoire, parts: List<String>, args: String?): String {
        // ... (code de getValeurNoeudParChemin inchangé, voir version précédente)
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
        // ... (code de getValeurJeuParChemin inchangé, voir version précédente)
        if (parts.isEmpty()) return "[CHEMIN_JEU_VIDE]"
        return when (parts[0].lowercase()) {
            "noeudactuelid" -> idNoeudActuel ?: "Aucun"
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
    private fun logDebug(message: String) = println("[MOTEUR DEBUG] $message")

    // --- Sauvegarde et Chargement ---

    /**
     * Sauvegarde l'état actuel du jeu dans un fichier.
     * @param nomFichier Le nom du fichier de sauvegarde (sans extension, ex: "partie1").
     * @param context Optionnel: Le Contexte Android, nécessaire pour l'écriture de fichier.
     * Si null, la fonction simulera la sauvegarde et affichera le JSON en log.
     * @return True si la sauvegarde a (potentiellement) réussi, false sinon.
     */
    fun sauvegarderPartie(nomFichier: String, context: Any? = null): Boolean {
        logInfo("Tentative de sauvegarde de la partie vers '$nomFichier.json'")
        try {
            val variablesLocalesASauvegarder = mutableMapOf<String, Map<String, String>>()
            mapNoeuds.forEach { (id, noeud) ->
                if (noeud.variablesLocalesAuNoeud.isNotEmpty()) {
                    variablesLocalesASauvegarder[id] = noeud.variablesLocalesAuNoeud.toMap() // Copie pour éviter la mutabilité
                }
            }

            val etatASauvegarder = EtatSauvegarde(
                joueurSerialise = joueur, // Joueur est une data class, Gson devrait bien la gérer
                idNoeudActuelSauvegarde = idNoeudActuel,
                consequencesEntreeTraiteesSauvegarde = consequencesEntreeTraiteesPourNoeudActuel,
                variablesLocalesDeTousLesNoeuds = variablesLocalesASauvegarder
            )

            val jsonSauvegarde = gson.toJson(etatASauvegarder)
            logDebug("JSON de sauvegarde généré:\n$jsonSauvegarde")

            // --- Début du code spécifique à Android pour l'écriture de fichier ---
            if (context != null && context is android.content.Context) {
                // Exemple d'écriture dans le stockage interne de l'application
                // Assurez-vous d'avoir les permissions nécessaires si vous écrivez ailleurs.
                val fichier = java.io.File(context.filesDir, "$nomFichier.json")
                fichier.writeText(jsonSauvegarde)
                logInfo("Partie sauvegardée avec succès dans ${fichier.absolutePath}")
            } else {
                logAvertissement("Contexte Android non fourni. Sauvegarde non écrite sur disque. JSON affiché en DEBUG.")
                // Pour un test hors Android, vous pourriez écrire dans un fichier local ici:
                // java.io.File("$nomFichier.json").writeText(jsonSauvegarde)
                // logInfo("Partie 'simulée' sauvegardée dans $nomFichier.json (environnement de test).")
            }
            // --- Fin du code spécifique à Android ---

            return true
        } catch (e: Exception) {
            logErreur("Erreur lors de la sauvegarde de la partie: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Charge l'état du jeu depuis un fichier de sauvegarde.
     * @param nomFichier Le nom du fichier de sauvegarde (sans extension, ex: "partie1").
     * @param context Optionnel: Le Contexte Android, nécessaire pour la lecture de fichier.
     * Si null, la fonction tentera de lire depuis le répertoire local (pour test).
     * @return True si le chargement a réussi, false sinon.
     */
    fun chargerPartie(nomFichier: String, context: Any? = null): Boolean {
        logInfo("Tentative de chargement de la partie depuis '$nomFichier.json'")
        try {
            var jsonSauvegarde: String? = null

            // --- Début du code spécifique à Android pour la lecture de fichier ---
            if (context != null && context is android.content.Context) {
                val fichier = java.io.File(context.filesDir, "$nomFichier.json")
                if (fichier.exists()) {
                    jsonSauvegarde = fichier.readText()
                } else {
                    logAvertissement("Fichier de sauvegarde '${fichier.absolutePath}' non trouvé.")
                    return false
                }
            } else {
                // Pour un test hors Android, essayez de lire depuis le répertoire local
                val fichierTest = java.io.File("$nomFichier.json")
                 if (fichierTest.exists()) {
                    jsonSauvegarde = fichierTest.readText()
                    logInfo("Chargement 'simulé' depuis ${fichierTest.absolutePath} (environnement de test).")
                } else {
                    logAvertissement("Fichier de sauvegarde de test '$nomFichier.json' non trouvé dans le répertoire local.")
                    return false
                }
            }
            // --- Fin du code spécifique à Android ---

            if (jsonSauvegarde == null || jsonSauvegarde.isBlank()) {
                logErreur("Le fichier de sauvegarde est vide ou n'a pas pu être lu.")
                return false
            }

            logDebug("JSON de sauvegarde lu:\n$jsonSauvegarde")

            val typeEtatSauvegarde = object : TypeToken<EtatSauvegarde>() {}.type
            val etatCharge = gson.fromJson<EtatSauvegarde>(jsonSauvegarde, typeEtatSauvegarde)

            // Restauration de l'état du moteur
            this.joueur = etatCharge.joueurSerialise // Remplace l'instance actuelle du joueur
            this.idNoeudActuel = etatCharge.idNoeudActuelSauvegarde
            this.consequencesEntreeTraiteesPourNoeudActuel = etatCharge.consequencesEntreeTraiteesSauvegarde

            // Restauration des variables locales des noeuds
            // Important: Cela suppose que la structure des noeuds (mapNoeuds) est déjà chargée
            // (par exemple, depuis la définition de l'histoire) avant de charger une partie.
            // Les variables locales sont restaurées SUR les noeuds existants.
            mapNoeuds.values.forEach { noeud ->
                noeud.variablesLocalesAuNoeud.clear() // Efface les anciennes variables locales du noeud en mémoire
                etatCharge.variablesLocalesDeTousLesNoeuds[noeud.id]?.let { variablesSauvegardeesPourCeNoeud ->
                    noeud.variablesLocalesAuNoeud.putAll(variablesSauvegardeesPourCeNoeud)
                }
            }

            logInfo("Partie chargée avec succès depuis '$nomFichier.json'. Noeud actuel: ${this.idNoeudActuel}")
            // Il pourrait être utile de rafraîchir l'UI ici ou de notifier l'UI que le jeu a été chargé.
            return true
        } catch (e: Exception) {
            logErreur("Erreur lors du chargement de la partie: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}
