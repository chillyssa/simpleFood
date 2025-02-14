package edu.msudenver.dailydish
/*
 * CS3013 - Mobile App Dev. - Summer 2022
 * Instructor: Thyago Mota
 * Student(s): Brea Chaney, Karent Correa and Alyssa Williams
 * Description: DailyDish - Ingredients Activity for user ingredients list
 */

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.msudenver.dailydish.Models.DBHelper
import edu.msudenver.dailydish.Models.Ingredient

class IngredientsActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    lateinit var recyclerView: RecyclerView
    lateinit var dbHelper: DBHelper
    private val ISO_FORMAT = DBHelper.ISO_FORMAT

    // Create and Ingredient Holder for the recycler view
    private inner class IngredientsHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtIngredient: TextView = view.findViewById(R.id.txtIngredient)
        val txtIngredientAmount: TextView = view.findViewById(R.id.txtIngredientAmount)
        val txtIngredientLocation: TextView = view.findViewById(R.id.txtIngredientLocation)
        val txtIngredientId: TextView = view.findViewById(R.id.txtIngredientId)

    }

    // Ingredient Adapter for the recycler view
    private inner class IngredientAdapter(
        var ingredientList: List<Ingredient>,
        var onClickListener: View.OnClickListener,
        var onLongClickListener: View.OnLongClickListener
    ) : RecyclerView.Adapter<IngredientsHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientsHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ingredients_list, parent, false)
            // OnClick and OnLongClick listeners for updating ingredient/deleting ingredient
            view.setOnClickListener(onClickListener)
            view.setOnLongClickListener(onLongClickListener)
            return IngredientsHolder(view)
        }

        override fun onBindViewHolder(holder: IngredientsHolder, position: Int) {
            // Set the views texts to the right values based on the ingredients table query
            val ingredient = ingredientList[position]
            holder.txtIngredient.text = ingredient.name
            holder.txtIngredientAmount.text = ingredient.quantity.toString() + " " + ingredient.unit
            holder.txtIngredientLocation.text = ingredient.categoryAsString()
            holder.txtIngredientId.text = ingredient.id.toString()
            ingredient.categoryAsString()

        }

        override fun getItemCount(): Int {
            return ingredientList.size
        }
    }

    // query the ingredients table and set each record to a view holder with recycler view
    fun populateRecyclerView() {
        val db = dbHelper.readableDatabase
        val ingredientList = mutableListOf<Ingredient>()
        val columns =
            arrayOf<String>("rowid, name, location, quantity, unit, addedDate, updatedDate")
        val cursor = db.query(
            "ingredients",
            columns,
            null,
            null,
            null,
            null,
            null
        )
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(0)
                val name = getString(1)
                val location = getInt(2)
                val quantity = getInt(3)
                val unit = getString(4)
                val createdDate = ISO_FORMAT.parse(getString(5))
                val updatedDate = ISO_FORMAT.parse(getString(6))
                val item = Ingredient(id, name, location, quantity, unit, createdDate, updatedDate)
                ingredientList.add(item)

            }
        }

        recyclerView.adapter = IngredientAdapter(ingredientList, this, this)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients)

        // Initialize db Helper and recyclerView
        dbHelper = DBHelper(this)
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        populateRecyclerView()

        // Get a reference to the FAB create button to insert new ingredients
        val fabCreate: FloatingActionButton = findViewById(R.id.fabCreate)
        fabCreate.setOnClickListener {
            // On click of the button, pass the activity to CreateUpdateActivity
            val intent = Intent(this, CreateUpdateActivity::class.java)
            intent.putExtra("op", CreateUpdateActivity.CREATE_OP)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        populateRecyclerView()
    }

    // If the recycler view of one ingredient is selected, pass the intent to CreateUpdateActivity to update the ingredient
    override fun onClick(v: View?) {
        if (v != null) {
            val rowid = v.findViewById<TextView>(R.id.txtIngredientId).text.toString().toInt()
            val intent = Intent(this, CreateUpdateActivity::class.java)
            intent.putExtra("op", CreateUpdateActivity.UPDATE_OP)
            intent.putExtra("rowid", rowid)
            startActivity(intent)

        }
    }

    // If the long click is performed, delete the ingredient, first alerting the user to confirm deletion
    override fun onLongClick(v: View?): Boolean {
        class MyDialogInterfaceListener(val id: Int) : DialogInterface.OnClickListener {

            override fun onClick(dialogInterface: DialogInterface?, which: Int) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    try {
                        val db = dbHelper.writableDatabase
                        db.execSQL(
                            """
                            DELETE FROM ingredients
                            WHERE rowid = $id
                        """
                        )
                        populateRecyclerView()

                    } catch (ex: SQLiteException) {
                        Log.i("Error: ", ex.toString())
                    }
                }
            }
        }

        if (v != null) {
            val desc = v.findViewById<TextView>(R.id.txtIngredient).text.toString()
            val id = v.findViewById<TextView>(R.id.txtIngredientId).text.toString().toInt()
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setMessage("Are you sure you want to delete the following item from your list?:\n\n${desc}")
            alertDialogBuilder.setPositiveButton("Yes", MyDialogInterfaceListener(id))
            alertDialogBuilder.setNegativeButton("No", MyDialogInterfaceListener(id))
            alertDialogBuilder.show()
            return true
        }
        return false
    }


}
