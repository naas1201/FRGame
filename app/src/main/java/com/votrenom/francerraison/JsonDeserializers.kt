package com.votrenom.francerraison // Adaptez à votre package

import com.google.gson.*
import java.lang.reflect.Type

class ConditionDeserializer : JsonDeserializer<Condition> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Condition {
        val jsonObject = json.asJsonObject
        // Vérifie si le champ "typeCondition" est présent, sinon c'est Condition.Aucun par défaut
        val typeConditionString = jsonObject.get("typeCondition")?.asString ?: return Condition.Aucun

        return when (typeConditionString.uppercase()) {
            "AUCUN" -> Condition.Aucun
            "STAT_NUMERIQUE" -> context.deserialize(jsonObject, Condition.StatNumerique::class.java)
            "DRAPEAU" -> context.deserialize(jsonObject, Condition.Drapeau::class.java)
            "OBJET_INVENTAIRE" -> context.deserialize(jsonObject, Condition.ObjetInventaire::class.java)
            "STATUT_VISA_FIANCEE" -> context.deserialize(jsonObject, Condition.StatutVisaFiancee::class.java)
            "PROFESSION_ACTUELLE_JOUEUR" -> context.deserialize(jsonObject, Condition.ProfessionActuelleJoueur::class.java)
            "NOM_PARTI_POLITIQUE_JOUEUR" -> context.deserialize(jsonObject, Condition.NomPartiPolitiqueJoueur::class.java)
            "ET" -> context.deserialize(jsonObject, Condition.Et::class.java)
            "OU" -> context.deserialize(jsonObject, Condition.Ou::class.java)
            "NON" -> context.deserialize(jsonObject, Condition.Non::class.java)
            else -> {
                System.err.println("[AVERTISSEMENT ConditionDeserializer] Type de condition inconnu: $typeConditionString. Retourne Condition.Aucun.")
                Condition.Aucun // Ou lancez une exception si vous préférez une gestion d'erreur plus stricte
            }
        }
    }
}

// Vous pourriez ajouter d'autres désérialiseurs personnalisés ici si nécessaire pour d'autres classes complexes.
