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
import androidx.compose.ui.platform.LocalContext
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
    var selectedCategory by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    
    // UI Feedback State
    var message by remember { mutableStateOf("") }
    var isSuccessMessage by remember { mutableStateOf(true) }

    val categories = listOf(
        "Comida" to Icons.Rounded.Restaurant,
        "Transporte" to Icons.Rounded.DirectionsCar,
        "Servicios" to Icons.Rounded.Lightbulb,
        "Salud" to Icons.Rounded.MedicalServices,
        "Ocio" to Icons.Rounded.Gamepad,
        "Otros" to Icons.Rounded.Category
    )

    val context = LocalContext.current
    val expenseStorage = remember(context) { ExpenseStorage(context) }
    val iconByCategory = remember(categories) { categories.toMap() }

    // Data State loaded from persistent storage.
    var expenses by remember {
        mutableStateOf(expenseStorage.loadExpenses(iconByCategory))
    }

    val categoryScrollState = rememberScrollState()

    LaunchedEffect(expenses) {
        expenseStorage.saveExpenses(expenses)
    }

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
                val totalAmount = expenses.sumOf { it.amount }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Total Gastado",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "C$ ${formatAmount(totalAmount)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
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
                            prefix = { Text("C$ ", fontWeight = FontWeight.Bold) },
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
                            .clip(MaterialTheme.shapes.large),
                        headlineContent = { Text(expense.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(expense.category) },
                        trailingContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "C$ ${formatAmount(expense.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    // EDIT
                                    editingId = expense.id
                                    expenseName = expense.name
                                    expenseAmount = expense.amount.toString()
                                    selectedCategory = expense.category
                                    message = ""
                                }) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    // DELETE
                                    expenses = expenses.filter { it.id != expense.id }
                                    if (editingId == expense.id) {
                                        editingId = null
                                        expenseName = ""
                                        expenseAmount = ""
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
