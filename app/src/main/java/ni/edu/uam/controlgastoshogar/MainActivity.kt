package ni.edu.uam.controlgastoshogar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ni.edu.uam.controlgastoshogar.ui.theme.ControlGastosHogarTheme
import java.util.Locale

data class ExpenseItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val icon: ImageVector
)

private fun parseAmount(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    val amount = normalized.toDoubleOrNull() ?: return null
    return amount.takeIf { it > 0 }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControlGastosHogarTheme {
                ExpenseControlScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseControlScreen() {
    // Form State
    var expenseName by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("C$") }
    var selectedCategory by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    
    // UI Feedback State
    var message by remember { mutableStateOf("") }
    var isSuccessMessage by remember { mutableStateOf(true) }
    
    // Data State
    var expenses by remember { mutableStateOf(listOf<ExpenseItem>()) }
    var monthlyBudget by remember { mutableStateOf(0.0) } // Presupuesto base en C$
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    // Tasas de cambio (simuladas: 1 USD = 36.62 NIO, 1 EUR = 39.50 NIO)
    val exchangeRates = mapOf("C$" to 1.0, "$" to 36.62, "€" to 39.50)

    fun convertToNio(amount: Double, currency: String): Double {
        return amount * (exchangeRates[currency] ?: 1.0)
    }

    val totalInNio = expenses.sumOf { convertToNio(it.amount, it.currency) }
    val remainingBudget = monthlyBudget - totalInNio
    val budgetProgress = if (monthlyBudget > 0) (totalInNio / monthlyBudget).coerceIn(0.0, 1.0) else 0.0

    val categories = listOf(
        "Comida" to Icons.Rounded.Restaurant,
        "Transporte" to Icons.Rounded.DirectionsCar,
        "Servicios" to Icons.Rounded.Lightbulb,
        "Salud" to Icons.Rounded.MedicalServices,
        "Ocio" to Icons.Rounded.Gamepad,
        "Otros" to Icons.Rounded.Category
    )

    val categoryScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Control de Gastos",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (expenses.isNotEmpty()) {
                            Text(
                                "Gestiona tus finanzas diarias",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. Dashboard Summary
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Presupuesto Mensual",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                OutlinedTextField(
                                    value = budgetInput,
                                    onValueChange = { 
                                        if (it.all { c -> c.isDigit() || c == '.' }) {
                                            budgetInput = it
                                            monthlyBudget = it.toDoubleOrNull() ?: 0.0
                                        }
                                    },
                                    placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)) },
                                    prefix = { Text("C$ ", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.width(200.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                        focusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                    ),
                                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    shape = MaterialTheme.shapes.medium,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                            Icon(
                                Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        LinearProgressIndicator(
                            progress = { budgetProgress.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = when {
                                budgetProgress > 0.9 -> MaterialTheme.colorScheme.error
                                budgetProgress > 0.7 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Gastado (Total)", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "C$ ${formatAmount(totalInNio)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalInNio > monthlyBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Disponible", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "C$ ${formatAmount(remainingBudget)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingBudget < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Presupuesto por Moneda (Mini chips)
            if (expenses.isNotEmpty()) {
                item {
                    val totals = expenses.groupBy { it.currency }
                        .mapValues { it.value.sumOf { item -> item.amount } }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        totals.forEach { (curr, total) ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text("$curr ${formatAmount(total)}") },
                                icon = { Icon(Icons.Rounded.Payments, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            // 2. CRUD Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingId == null) "Nuevo Gasto" else "Editar Gasto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (editingId != null) {
                                TextButton(onClick = {
                                    editingId = null
                                    expenseName = ""
                                    expenseAmount = ""
                                    selectedCurrency = "C$"
                                    selectedCategory = ""
                                }) {
                                    Text("Cancelar")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = expenseName,
                            onValueChange = { expenseName = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = { Icon(Icons.Rounded.EditNote, null) },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = expenseAmount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    if (input.count { it == '.' || it == ',' } <= 1) expenseAmount = input
                                }
                            },
                            label = { Text("Monto") },
                            leadingIcon = {
                                val currencyOptions = listOf(
                                    "C$" to "Córdobas",
                                    "$" to "Dólares",
                                    "€" to "Euros"
                                )
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable { currencyMenuExpanded = true }
                                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = selectedCurrency,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = currencyMenuExpanded,
                                        onDismissRequest = { currencyMenuExpanded = false }
                                    ) {
                                        currencyOptions.forEach { (symbol, name) ->
                                            DropdownMenuItem(
                                                text = { Text("$symbol ($name)") },
                                                onClick = {
                                                    selectedCurrency = symbol
                                                    currencyMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        // Improved Horizontal Scroll for selections
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Categoría",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(categoryScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { (name, icon) ->
                                    FilterChip(
                                        selected = selectedCategory == name,
                                        onClick = { selectedCategory = name },
                                        label = { Text(name) },
                                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
                                        shape = CircleShape
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val trimmedName = expenseName.trim()
                                val parsedAmount = parseAmount(expenseAmount)
                                val categoryIcon = categories.find { it.first == selectedCategory }?.second ?: Icons.Rounded.Category

                                when {
                                    trimmedName.isBlank() -> {
                                        isSuccessMessage = false
                                        message = "Indica una descripción"
                                    }
                                    selectedCategory.isBlank() -> {
                                        isSuccessMessage = false
                                        message = "Elige una categoría"
                                    }
                                    parsedAmount == null -> {
                                        isSuccessMessage = false
                                        message = "Monto inválido"
                                    }
                                    else -> {
                                        if (editingId == null) {
                                            // CREATE
                                            expenses = expenses + ExpenseItem(
                                                name = trimmedName,
                                                amount = parsedAmount,
                                                currency = selectedCurrency,
                                                category = selectedCategory,
                                                icon = categoryIcon
                                            )
                                            message = "¡Gasto guardado!"
                                        } else {
                                            // UPDATE
                                            expenses = expenses.map { 
                                                if (it.id == editingId) it.copy(
                                                    name = trimmedName,
                                                    amount = parsedAmount,
                                                    currency = selectedCurrency,
                                                    category = selectedCategory,
                                                    icon = categoryIcon
                                                ) else it
                                            }
                                            message = "¡Gasto actualizado!"
                                            editingId = null
                                        }
                                        isSuccessMessage = true
                                        expenseName = ""
                                        expenseAmount = ""
                                        selectedCurrency = "C$"
                                        selectedCategory = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(if (editingId == null) Icons.Rounded.Add else Icons.Rounded.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (editingId == null) "Guardar Gasto" else "Actualizar Gasto")
                        }
                    }
                }
            }

            // 3. Feedback Message
            item {
                AnimatedVisibility(
                    visible = message.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = if (isSuccessMessage) MaterialTheme.colorScheme.secondaryContainer 
                                else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().clickable { message = "" }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (isSuccessMessage) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (isSuccessMessage) MaterialTheme.colorScheme.onSecondaryContainer 
                                       else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isSuccessMessage) MaterialTheme.colorScheme.onSecondaryContainer 
                                        else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 4. List of Expenses (Read/Update/Delete)
            if (expenses.isNotEmpty()) {
                item {
                    Text(
                        text = "Historial Completo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(expenses.asReversed(), key = { it.id }) { expense ->
                    ListItem(
                        modifier = Modifier
                            .animateItem()
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                // Populating for UPDATE
                                editingId = expense.id
                                expenseName = expense.name
                                expenseAmount = expense.amount.toString()
                                selectedCurrency = expense.currency
                                selectedCategory = expense.category
                                message = ""
                            },
                        headlineContent = { Text(expense.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(expense.category) },
                        trailingContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${expense.currency} ${formatAmount(expense.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    // DELETE
                                    expenses = expenses.filter { it.id != expense.id }
                                    if (editingId == expense.id) {
                                        editingId = null
                                        expenseName = ""
                                        expenseAmount = ""
                                        selectedCurrency = "C$"
                                        selectedCategory = ""
                                    }
                                }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Borrar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(expense.icon, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No hay gastos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseControlPreview() {
    ControlGastosHogarTheme {
        ExpenseControlScreen()
    }
}
