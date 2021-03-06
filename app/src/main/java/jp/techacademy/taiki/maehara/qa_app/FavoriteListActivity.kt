package jp.techacademy.taiki.maehara.qa_app

import  android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*



class FavoriteListActivity : AppCompatActivity() {

//    private var mGenre = 1

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    // user_id
    private val mUserId = FirebaseAuth.getInstance().currentUser!!.uid

    //    private var mGenreRef: DatabaseReference? = null
    private var mFavoriteRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

//            val answerArrayList = ArrayList<Answer>()
            var answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as ArrayList<Answer>?
//            if (answerMap != null) {
//                for (key in answerMap.keys) {
//                    val temp = answerMap[key] as Map<String, String>
//                    val answerBody = temp["body"] ?: ""
//                    val answerName = temp["name"] ?: ""
//                    val answerUid = temp["uid"] ?: ""
//                    val answer = Answer(answerBody, answerName, answerUid, key)
//                    answerArrayList.add(answer)
//                }
//            }

            val genre = map["genre"].toString().toInt()

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                genre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // ??????????????????Question?????????
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // ??????????????????????????????????????????????????????????????????Answer)??????
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)

//        setSupportActionBar(toolbar)
//
//        fab.setOnClickListener { view ->
//
//            // ????????????????????????????????????????????????
//            val user = FirebaseAuth.getInstance().currentUser
//
//            // ?????????????????????????????????????????????????????????????????????
//            if (user == null) {
//                val intent = Intent(applicationContext, LoginActivity::class.java)
//                startActivity(intent)
//            } else {
//                // ?????????????????????????????????????????????????????????
//                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
//                intent.putExtra("genre", mGenre)
//                startActivity(intent)
//            }
//        }

//        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
//        drawer_layout.addDrawerListener(toggle)
//        toggle.syncState()
//
//        nav_view.setNavigationItemSelectedListener(this)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListView?????????
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener{parent, view, position, id ->
            // Question??????????????????????????????????????????????????????????????????
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
//        // ????????????????????????????????????????????????Adapter??????????????????Adapter???ListView?????????????????????
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

//        // ??????????????????????????????????????????????????????
//        if (mGenreRef != null) {
//            mGenreRef!!.removeEventListener(mEventListener)
//        }

        mFavoriteRef = mDatabaseReference.child(FavoritesPATH).child(mUserId)
        mFavoriteRef!!.addChildEventListener(mEventListener)

    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//
//        if (id == R.id.action_settings) {
//            val intent = Intent(applicationContext, SettingActivity::class.java)
//            startActivity(intent)
//            return true
//        }
//
//        return super.onOptionsItemSelected(item)
//    }

//    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//
//        if (id == R.id.nav_hobby) {
//            toolbar.title = getString(R.string.menu_hobby_label)
//            mGenre = 1
//        } else if (id == R.id.nav_life) {
//            toolbar.title = getString(R.string.menu_life_label)
//            mGenre = 2
//        } else if (id == R.id.nav_health) {
//            toolbar.title = getString(R.string.menu_health_label)
//            mGenre = 3
//        } else if (id == R.id.nav_computer) {
//            toolbar.title = getString(R.string.menu_computer_label)
//            mGenre = 4
//        } else if (id == R.id.nav_favorite) {
//            toolbar.title = getString(R.string.menu_favorite_label)
//
//        }
//
//        drawer_layout.closeDrawer(GravityCompat.START)
//


//        return true
//    }


}