package com.example.lab_week_09

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab_week_09.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// ---------------------------------------------------------
// MainActivity
// ---------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LAB_WEEK_09Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    App(navController = navController)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Data class
// ---------------------------------------------------------
data class Student(
    var name: String
)

// ---------------------------------------------------------
// Home ( menerima NavHostController langsung )
// ---------------------------------------------------------
@Composable
fun Home(navController: NavHostController) {
    val listData = remember {
        mutableStateListOf(
            Student("Tanu"),
            Student("Tina"),
            Student("Tono")
        )
    }

    var inputField by remember { mutableStateOf(Student("")) }
    val context = LocalContext.current

    HomeContent(
        listData = listData,
        inputField = inputField,
        onInputValueChange = { inputField = Student(it) },
        onButtonClick = {
            // cegah submit kosong + tampilkan Toast
            if (inputField.name.isNotBlank()) {
                listData.add(Student(inputField.name))
                inputField = Student("")
            } else {
                Toast.makeText(context, "Please enter a name first!", Toast.LENGTH_SHORT).show()
            }
        },
        navigateFromHomeToResult = {
            // Konversi list ke JSON pakai Moshi (dengan Kotlin adapter)
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Student::class.java)
            val adapter = moshi.adapter<List<Student>>(type)
            val json = adapter.toJson(listData)

            // Simpan JSON ke savedStateHandle, lalu navigasi tanpa membawa JSON lewat URL
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("listData", json)

            navController.navigate("resultContent")
        }
    )
}

// ---------------------------------------------------------
// HomeContent (UI utama di Home)
// ---------------------------------------------------------
@Composable
fun HomeContent(
    listData: SnapshotStateList<Student>,
    inputField: Student,
    onInputValueChange: (String) -> Unit,
    onButtonClick: () -> Unit,
    navigateFromHomeToResult: () -> Unit
) {
    LazyColumn {
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBackgroundTitleText(
                    text = stringResource(id = R.string.enter_item)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = inputField.name,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    onValueChange = { onInputValueChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PrimaryTextButton(
                        text = stringResource(id = R.string.button_click)
                    ) {
                        onButtonClick()
                    }
                    PrimaryTextButton(
                        text = stringResource(id = R.string.button_navigate)
                    ) {
                        navigateFromHomeToResult()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(listData) { item ->
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBackgroundItemText(text = item.name)
            }
        }
    }
}

// ---------------------------------------------------------
// App (navigation graph)
// ---------------------------------------------------------
// ---------------------------------------------------------
// App (navigation graph) - [PERBAIKAN]
// ---------------------------------------------------------
@Composable
fun App(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            Home(navController)
        }

        // route tanpa argument string panjang; ambil JSON dari savedStateHandle
        composable("resultContent") { /* 'backStackEntry' di sini tidak kita perlukan */

            // [PERBAIKAN ADA DI SINI]
            // Ambil data dari SavedStateHandle milik screen SEBELUMNYA ("home")
            val json = navController.previousBackStackEntry // <-- Ini adalah "home"
                ?.savedStateHandle // Ambil "bagasi" milik "home"
                ?.get<String>("listData") // Ambil data "listData"
                .orEmpty() // Jika null, jadikan string kosong

            ResultContent(json)
        }
    }
}

// ---------------------------------------------------------
// ResultContent (decode JSON + tampilkan dengan LazyColumn)
// ---------------------------------------------------------
@Composable
fun ResultContent(listData: String) {
    // decode JSON ke List<Student> aman (try/catch)
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, Student::class.java)
    val adapter = moshi.adapter<List<Student>>(type)

    val students = remember(listData) {
        try {
            adapter.fromJson(listData) ?: emptyList()
        } catch (e: Exception) {
            emptyList<Student>()
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnBackgroundTitleText(text = "Result Content")

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(students) { student ->
                OnBackgroundItemText(text = student.name)
            }
        }
    }
}

// ---------------------------------------------------------
// Preview (gunakan rememberNavController agar preview dapat memanggil Home)
// ---------------------------------------------------------
@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    val navController = rememberNavController()
    LAB_WEEK_09Theme {
        Home(navController)
    }
}
