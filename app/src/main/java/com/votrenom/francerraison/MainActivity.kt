package com.votrenom.francerraison // Adaptez à votre package

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding // Pour ajouter du padding aux boutons dynamiques
import com.google.gson.GsonBuilder // Import pour GsonBuilder
import com.google.gson.reflect.TypeToken // Import pour TypeToken

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
            // Avant de charger, l'histoire de base (mapNoeuds) doit idéalement être la même que lors de la sauvegarde.
            // Notre MoteurJeu.chargerPartie restaure l'état sur la structure de noeuds actuellement chargée.
            // Si vous voulez permettre de charger une sauvegarde même si la définition de l'histoire a un peu changé
            // (ex: textes corrigés mais IDs de noeuds stables), c'est généralement ok.
            // Si la structure des noeuds (IDs) change radicalement, les sauvegardes peuvent devenir incompatibles.

            // Pour s'assurer que la structure de l'histoire est chargée avant de restaurer l'état dessus :
            // On pourrait re-initialiser joueur et moteur puis charger l'histoire JSON avant de charger la sauvegarde.
            // Pour l'instant, on charge sur l'état actuel du moteur (qui a déjà chargé l'histoire JSON via initialiserJeu au lancement).
            if (moteurJeu.chargerPartie("partie_fr", this)) {
                Toast.makeText(this, "Partie chargée !", Toast.LENGTH_SHORT).show()
                actualiserAffichageNoeud() // Crucial pour refléter l'état chargé
            } else {
                Toast.makeText(this, "Erreur lors du chargement ou sauvegarde non trouvée.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initialiserJeu() {
        joueur = Joueur(nom = "Jean Dupont") // Ou restaurez un joueur par défaut si nécessaire
        moteurJeu = MoteurJeu(joueur)

        // Configuration de Gson avec notre désérialiseur personnalisé pour Condition
        val gson = GsonBuilder()
            .registerTypeAdapter(Condition::class.java, ConditionDeserializer())
            // .setPrettyPrinting() // Optionnel: pour un JSON plus lisible lors de la sauvegarde (peut impacter légèrement la perf)
            .create()

        // Chargement de l'histoire depuis le fichier JSON dans les assets
        val jsonHistoire = chargerJsonDepuisAssets("histoire_principale.json", this) // Utilise FileUtils.kt

        if (jsonHistoire != null) {
            // Définition du type pour la désérialisation d'une liste de NoeudHistoire
            val typeListeNoeudHistoire = object : TypeToken<List<NoeudHistoire>>() {}.type
            try {
                val histoireChargee: List<NoeudHistoire> = gson.fromJson(jsonHistoire, typeListeNoeudHistoire)
                
                if (histoireChargee.isNotEmpty()) {
                    moteurJeu.chargerHistoire(histoireChargee)
                    if (moteurJeu.commencerJeu("debut")) { // Assurez-vous que "debut" existe dans votre JSON
                        actualiserAffichageNoeud()
                    } else {
                        textViewStoryDescription.text = "Erreur: Impossible de démarrer l'histoire. Le noeud initial 'debut' est-il présent dans 'histoire_principale.json' et la structure de l'histoire est-elle valide ?"
                        linearLayoutChoices.removeAllViews()
                    }
                } else {
                    textViewStoryDescription.text = "Erreur: Le fichier d'histoire 'histoire_principale.json' semble vide ou mal structuré (aucune histoire chargée)."
                    linearLayoutChoices.removeAllViews()
                }
            } catch (e: Exception) {
                e.printStackTrace() // Très important pour le débogage des erreurs JSON
                textViewStoryDescription.text = "Erreur critique lors du parsing du JSON de l'histoire : ${e.localizedMessage}.\nVérifiez la console Logcat pour les détails et la structure de votre fichier 'histoire_principale.json'."
                linearLayoutChoices.removeAllViews()
            }
        } else {
            textViewStoryDescription.text = "Erreur: Impossible de charger le fichier d'histoire 'histoire_principale.json' depuis les assets. Le fichier existe-t-il bien dans le dossier 'app/src/main/assets/' ?"
            linearLayoutChoices.removeAllViews()
        }
    }

    private fun actualiserAffichageNoeud() {
        val noeudActuel = moteurJeu.getNoeudActuel()

        if (noeudActuel == null) {
            // Gérer le cas où aucun noeud n'est actif (peut être une fin de jeu normale ou une erreur)
            val dernierIdNoeud = moteurJeu.idNoeudActuel // Peut être null si le jeu n'a jamais vraiment commencé
            if (dernierIdNoeud == null && moteurJeu.getNoeudActuel() == null && mapNoeuds.isNotEmpty()) { // mapNoeuds n'est pas directement accessible ici, vérifier autrement.
                 textViewStoryDescription.text = "Fin de la narration ou problème de chargement du noeud initial."
            } else {
                 textViewStoryDescription.text = "FIN DE L'HISTOIRE." // Message générique de fin
            }

            linearLayoutChoices.removeAllViews()
            buttonSave.isEnabled = false // Souvent, on ne sauvegarde pas sur un écran de fin.
            return
        }
        buttonSave.isEnabled = true

        textViewStoryDescription.text = moteurJeu.getDescriptionNoeudActuelFormattee()
        scrollViewDescription.post { scrollViewDescription.fullScroll(ScrollView.FOCUS_UP) }

        linearLayoutChoices.removeAllViews()
        val choixVisibles = moteurJeu.getChoixVisiblesNoeudActuel()

        if (choixVisibles.isEmpty()) {
            // Si pas de choix, vérifier si c'est une fin de jeu déclarée ou une impasse.
            if (noeudActuel.typeDeNoeud in listOf(TypeNoeud.FIN_DE_JEU_POSITIVE, TypeNoeud.FIN_DE_JEU_NEGATIVE, TypeNoeud.FIN_DE_JEU_NEUTRE)) {
                // C'est une fin, la description du noeud suffit. Pas besoin de message d'impasse.
            } else {
                // Ce n'est pas une fin déclarée mais il n'y a pas de choix. C'est une impasse.
                val textViewImpasse = TextView(this).apply {
                    text = "... Il n'y a plus rien à faire ici pour le moment."
                    // Vous pouvez ajouter un style ici
                }
                linearLayoutChoices.addView(textViewImpasse)
            }
        } else {
            choixVisibles.forEachIndexed { index, choix ->
                val button = Button(this).apply {
                    text = choix.texteAffichage
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        // Assurez-vous que R.dimen.choice_button_margin_top est défini dans res/values/dimens.xml
                        it.topMargin = try { resources.getDimensionPixelSize(R.dimen.choice_button_margin_top) } catch (e: Exception) { 16 } // fallback
                    }
                    // Utiliser une dimension pour le padding aussi pour la cohérence
                    val paddingValue = try { resources.getDimensionPixelSize(R.dimen.choice_button_margin_top) } catch (e: Exception) { 16 }
                    setPadding(paddingValue, paddingValue, paddingValue, paddingValue)

                    setOnClickListener {
                        val succes = moteurJeu.faireChoix(index)
                        if (succes) {
                            actualiserAffichageNoeud()
                        } else {
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

    // La fonction creerHistoireDeTest() a été supprimée car l'histoire est maintenant chargée depuis JSON.
}
