package com.votrenom.francerraison // Adaptez à votre package

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast // Pour afficher des messages simples
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding // Pour ajouter du padding aux boutons dynamiques

class MainActivity : AppCompatActivity() {

    // Déclaration tardive des vues de l'interface utilisateur
    private lateinit var textViewStoryDescription: TextView
    private lateinit var linearLayoutChoices: LinearLayout
    private lateinit var buttonSave: Button
    private lateinit var buttonLoad: Button
    private lateinit var scrollViewDescription: ScrollView

    // Instance de notre moteur de jeu et du joueur
    private lateinit var moteurJeu: MoteurJeu
    private lateinit var joueur: Joueur

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des vues
        textViewStoryDescription = findViewById(R.id.textViewStoryDescription)
        linearLayoutChoices = findViewById(R.id.linearLayoutChoices)
        buttonSave = findViewById(R.id.buttonSave)
        buttonLoad = findViewById(R.id.buttonLoad)
        scrollViewDescription = findViewById(R.id.scrollViewDescription)

        // Initialisation du joueur et du moteur de jeu
        initialiserJeu()

        // Configuration des listeners pour les boutons de sauvegarde et chargement
        buttonSave.setOnClickListener {
            if (moteurJeu.sauvegarderPartie("partie_fr", this)) {
                Toast.makeText(this, "Partie sauvegardée !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erreur lors de la sauvegarde.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLoad.setOnClickListener {
            if (moteurJeu.chargerPartie("partie_fr", this)) {
                Toast.makeText(this, "Partie chargée !", Toast.LENGTH_SHORT).show()
                // Après le chargement, il faut rafraîchir l'affichage avec le nouvel état
                actualiserAffichageNoeud()
            } else {
                Toast.makeText(this, "Erreur lors du chargement ou sauvegarde non trouvée.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initialiserJeu() {
        joueur = Joueur(nom = "Jean Dupont") // Utilisez les valeurs par défaut ou personnalisez ici
        moteurJeu = MoteurJeu(joueur)

        // Définition d'une histoire de test simple
        val histoireTest = creerHistoireDeTest()
        moteurJeu.chargerHistoire(histoireTest)

        if (moteurJeu.commencerJeu("debut")) { // Assurez-vous que votre histoire a un noeud avec l'ID "debut"
            actualiserAffichageNoeud()
        } else {
            textViewStoryDescription.text = "Erreur: Impossible de démarrer l'histoire. Vérifiez l'ID du noeud initial et la structure de l'histoire."
        }
    }

    private fun actualiserAffichageNoeud() {
        val noeudActuel = moteurJeu.getNoeudActuel()

        if (noeudActuel == null) {
            textViewStoryDescription.text = "FIN DE L'HISTOIRE (ou erreur)."
            linearLayoutChoices.removeAllViews() // Plus de choix
            // Vous pourriez vouloir désactiver les boutons save/load ici aussi
            buttonSave.isEnabled = false
            buttonLoad.isEnabled = false // Peut-être permettre de charger une autre partie ou de recommencer?
            return
        }
        buttonSave.isEnabled = true // Réactiver si on arrive sur un noeud valide

        // Met à jour la description de l'histoire
        // La méthode remplacerPlaceholders est dans MoteurJeu, mais ici on récupère le texte déjà formaté
        textViewStoryDescription.text = moteurJeu.getDescriptionNoeudActuelFormattee()
        // Faire défiler le ScrollView vers le haut pour voir le début du nouveau texte
        scrollViewDescription.post { scrollViewDescription.fullScroll(ScrollView.FOCUS_UP) }


        // Met à jour les boutons de choix
        linearLayoutChoices.removeAllViews() // Supprime les anciens boutons de choix

        val choixVisibles = moteurJeu.getChoixVisiblesNoeudActuel()
        if (choixVisibles.isEmpty() && !noeudActuel.estUneFinLogique()) {
             // S'il n'y a pas de choix mais que ce n'est pas une fin logique déclarée,
             // cela peut indiquer une impasse dans l'histoire.
             val textViewImpasse = TextView(this).apply {
                 text = "Vous vous trouvez dans une impasse..."
                 // Stylez ce TextView si nécessaire
             }
            linearLayoutChoices.addView(textViewImpasse)
        } else {
            choixVisibles.forEachIndexed { index, choix ->
                val button = Button(this).apply {
                    text = choix.texteAffichage
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.topMargin = resources.getDimensionPixelSize(R.dimen.choice_button_margin_top) // Nécessite de définir cette dimen
                    }
                    setPadding(16) // Ajoute un peu de padding interne
                    // Stylez le bouton ici si nécessaire (couleur de fond, texte, etc.)
                    // setBackgroundResource(R.drawable.custom_button_background) // Exemple

                    setOnClickListener {
                        val succes = moteurJeu.faireChoix(index)
                        if (succes) {
                            actualiserAffichageNoeud()
                        } else {
                            // Gérer l'échec du traitement du choix (devrait être rare si la logique est bonne)
                            Toast.makeText(this@MainActivity, "Erreur lors du traitement du choix.", Toast.LENGTH_SHORT).show()
                            textViewStoryDescription.append("\n\n[Erreur interne lors du traitement du choix. Le jeu ne peut continuer sur cette branche.]")
                            linearLayoutChoices.removeAllViews()
                        }
                    }
                }
                linearLayoutChoices.addView(button)
            }
        }
    }

    // --- Création de l'histoire de test ---
    // Dans un vrai jeu, vous chargeriez ceci depuis un fichier JSON dans le dossier assets
    private fun creerHistoireDeTest(): List<NoeudHistoire> {
        return listOf(
            NoeudHistoire(
                id = "debut",
                titre = "Perte d'emploi",
                description = "Vous êtes Jean Dupont, consultant français aux Philippines. Votre manager vous convoque : 'Jean, mauvaises nouvelles. Restructuration. Votre poste est supprimé.' Vous perdez votre emploi. Le retour en France semble inévitable.\nVotre moral ({joueur.statsNumeriques.moral}) en prend un coup.",
                consequencesEntree = listOf(
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "moral", "-20")
                ),
                choix = listOf(
                    ChoixHistoire("Organiser immédiatement le retour en France.", "retour_france",
                        consequences = listOf(Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "stress", "10"))),
                    ChoixHistoire("Prendre quelques jours pour encaisser et réfléchir.", "reflexion_philippines",
                        consequences = listOf(Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "tempsDisponible", "-3")))
                )
            ),
            NoeudHistoire(
                id = "reflexion_philippines",
                titre = "Quelques jours de répit",
                description = "Vous passez quelques jours à Manille, entre les démarches administratives et les appels à votre fiancée, Maria. Elle est médecin et espère toujours pouvoir vous rejoindre en France un jour. La situation est stressante.\nLe temps passe ({joueur.statsNumeriques.tempsDisponible}). Votre stress ({joueur.statsNumeriques.stress}) augmente un peu.",
                 consequencesEntree = listOf(
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "stress", "5")
                ),
                choix = listOf(
                    ChoixHistoire("Contacter Maria pour discuter sérieusement de l'avenir.", "appel_maria_serieux"),
                    ChoixHistoire("Finaliser les préparatifs pour rentrer en France seul pour l'instant.", "retour_france")
                )
            ),
            NoeudHistoire(
                id = "retour_france",
                titre = "Retour en Métropole",
                description = "L'avion atterrit à Paris. Le choc culturel est rude après des années aux Philippines. Vous vous installez temporairement chez vos parents. La France que vous retrouvez semble... changée, plus tendue. Votre 'moral' ({joueur.statsNumeriques.moral}) est bas, mais votre 'determination' ({joueur.statsNumeriques.determination}) à reconstruire votre vie est là.",
                consequencesEntree = listOf(
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "moral", "-10"),
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "determination", "5"),
                    Consequence(TypeConsequence.AJOUTER_DRAPEAU_UNIQUE, "EST_EN_FRANCE")
                ),
                choix = listOf(
                    ChoixHistoire("Commencer les démarches pour le visa de Maria.", "demarche_visa_maria",
                        condition = Condition.Drapeau("EST_EN_FRANCE")
                    ),
                    ChoixHistoire("Chercher un nouveau travail de consultant en France.", "recherche_emploi_fr",
                         consequences = listOf(Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "tempsDisponible", "-5")))
                )
            ),
            NoeudHistoire(
                id = "demarche_visa_maria",
                titre = "Le parcours du combattant",
                description = "Vous entamez les démarches pour le visa de Maria. L'administration française est un labyrinthe. Les semaines passent. Un jour, la lettre tombe : REFUS. Motif : 'Risque de non-retour'. Maria est dévastée. Une colère froide ({joueur.statsNumeriques.colereJustifiee}) monte en vous. Votre 'determination' ({joueur.statsNumeriques.determination}) explose.",
                consequencesEntree = listOf(
                    Consequence(TypeConsequence.CHANGER_STATUT_VISA_FIANCEE, "fianceeVisaStatut", "REFUSE"),
                    Consequence(TypeConsequence.AJOUTER_DRAPEAU_UNIQUE, "FIANCEE_VISA_REFUSE"),
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "colereJustifiee", "50"),
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "determination", "40"),
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "moral", "-30")
                ),
                choix = listOf(
                    ChoixHistoire("Se résigner et chercher un moyen pour Maria de faire appel.", "appel_visa",
                        condition = Condition.StatutVisaFiancee(VisaStatut.REFUSE)),
                    ChoixHistoire("Cette injustice est trop forte ! Le système est fautif. Il faut le changer.", "decision_politique",
                        condition = Condition.StatutVisaFiancee(VisaStatut.REFUSE))
                )
            ),
            NoeudHistoire(
                id = "decision_politique",
                titre = "La Graine de la Révolte",
                description = "Le refus du visa de Maria est la goutte d'eau. Vous réalisez que les problèmes de la France vous touchent directement. Vous décidez d'étudier le droit et de vous engager pour changer les choses.\nVotre 'connaissanceJuridique' ({joueur.statsNumeriques.connaissanceJuridique}) est faible, mais votre 'determination' ({joueur.statsNumeriques.determination}) est à son comble.",
                consequencesEntree = listOf(
                    Consequence(TypeConsequence.CHANGER_PROFESSION_JOUEUR, "professionActuelle", "Étudiant en Droit autodidacte")
                ),
                choix = listOf(
                    ChoixHistoire("Commencer à étudier le droit constitutionnel français.", "etude_droit",
                         consequences = listOf(Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "tempsDisponible", "-10"))),
                    ChoixHistoire("Contacter d'anciens collègues pour sonder le terrain politique.", "sonder_politique",
                        consequences = listOf(Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "tempsDisponible", "-3")))
                )
            ),
            // Ajoutez d'autres noeuds ici pour continuer l'histoire...
            NoeudHistoire(
                id = "appel_visa",
                titre = "Voies de Recours",
                description = "Faire appel sera long et coûteux. Mais pour Maria, vous êtes prêt à tout.",
                choix = emptyList(), // Fin de cette branche pour l'exemple
                typeDeNoeud = TypeNoeud.FIN_DE_JEU_NEUTRE
            ),
             NoeudHistoire(
                id = "etude_droit",
                titre = "Plongée dans les Codes",
                description = "Les nuits sont longues, passées sur les articles de loi. Votre connaissance juridique ({joueur.statsNumeriques.connaissanceJuridique}) augmente lentement.",
                consequencesEntree = listOf(
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "connaissanceJuridique", "15"),
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "fatiguePhysique", "10")
                ),
                choix = emptyList(), // Fin de cette branche pour l'exemple
                typeDeNoeud = TypeNoeud.FIN_DE_JEU_NEUTRE
            ),
            NoeudHistoire(
                id = "recherche_emploi_fr",
                titre = "Retour à la case départ",
                description = "La recherche d'emploi est plus difficile que prévu. Le marché est saturé.",
                choix = emptyList(),
                typeDeNoeud = TypeNoeud.FIN_DE_JEU_NEUTRE
            ),
            NoeudHistoire(
                id = "sonder_politique",
                titre = "Premiers Contacts",
                description = "Certains contacts sont intéressés, d'autres méfiants. Le chemin sera long.",
                choix = emptyList(),
                typeDeNoeud = TypeNoeud.FIN_DE_JEU_NEUTRE
            ),
            NoeudHistoire( // Pour le test de appel_maria_serieux
                id = "appel_maria_serieux",
                description = "La discussion avec Maria est intense. Vous réaffirmez votre engagement. Votre relation ({joueur.statsNumeriques.relationFiancee}) se renforce.",
                 consequencesEntree = listOf(
                    Consequence(TypeConsequence.MODIFIER_STAT_NUMERIQUE, "relationFiancee", "10")
                ),
                choix = listOf(ChoixHistoire("Continuer à préparer le retour en France.", "retour_france")),
                typeDeNoeud = TypeNoeud.STANDARD
            )
        )
    }
}
