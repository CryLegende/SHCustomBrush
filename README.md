<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHCustomBrush

*Ajoute des items custom au loot du brossage des blocs suspects.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Ajoute des items custom au loot du brossage des blocs suspects.

Utilisé sur : **Opale**.

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/shcustombrush add <nom> <chance>` | — | Ajoute l'objet en main à la table de loot (le remplace s'il existe). | `shcustombrush.admin.add` |
| `/shcustombrush remove <nom>` | — | Retire une entrée. | `shcustombrush.admin.remove` |
| `/shcustombrush list` | — | Liste les entrées, leur poids et leur part du total. | `shcustombrush.admin.list` |
| `/shcustombrush reload` | — | Relit `config.yml` et `messages.yml`. Un fichier illisible ne change rien. | `shcustombrush.admin.reload` |

Sans argument, ou avec une sous-commande inconnue, une ligne d'usage ne liste que les sous-commandes permises à l'expéditeur. Seul `add` demande un joueur (objet en main) ; les autres marchent depuis la console.

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shcustombrush.admin` | Toute l'administration (parent des quatre nœuds suivants). | `op` |
| `shcustombrush.admin.add` | Ajouter l'objet en main à la table de loot. | `op` |
| `shcustombrush.admin.remove` | Retirer une entrée. | `op` |
| `shcustombrush.admin.list` | Lister la table de loot. | `op` |
| `shcustombrush.admin.reload` | Recharger la configuration. | `op` |

`config.yml` contient à la fois les réglages et la table `LootTable`, que `add` et `remove` réécrivent ; les commentaires du fichier sont conservés.

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHCustomBrush/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
