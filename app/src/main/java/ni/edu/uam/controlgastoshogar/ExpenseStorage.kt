package ni.edu.uam.controlgastoshogar

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray

private const val EXPENSE_PREFS = "expense_prefs"
private const val EXPENSES_KEY = "expenses_json"

class ExpenseStorage(context: Context) {
    private val prefs = context.getSharedPreferences(EXPENSE_PREFS, Context.MODE_PRIVATE)

    fun saveExpenses(expenses: List<ExpenseItem>) {
        val array = JSONArray()
        expenses.forEach { expense ->
            array.put(
                org.json.JSONObject()
                    .put("id", expense.id)
                    .put("name", expense.name)
                    .put("amount", expense.amount)
                    .put("currency", expense.currency)
                    .put("category", expense.category)
            )
        }

        prefs.edit().putString(EXPENSES_KEY, array.toString()).apply()
    }

    fun loadExpenses(iconByCategory: Map<String, ImageVector>): List<ExpenseItem> {
        val raw = prefs.getString(EXPENSES_KEY, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optLong("id", System.currentTimeMillis() + index)
                    val name = item.optString("name", "").trim()
                    val amount = item.optDouble("amount", -1.0)
                    val currency = item.optString("currency", "C$").trim().ifBlank { "C$" }
                    val category = item.optString("category", "").trim()

                    if (name.isBlank() || category.isBlank() || amount <= 0.0) continue

                    add(
                        ExpenseItem(
                            id = id,
                            name = name,
                            amount = amount,
                            currency = currency,
                            category = category,
                            icon = iconByCategory[category] ?: Icons.Rounded.Category
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

