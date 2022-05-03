package jp.techacademy.taiki.maehara.qa_app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_answer_send.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_send.*
import kotlinx.android.synthetic.main.activity_question_send.progressBar
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            menuInflater.inflate(R.menu.menu_question_detail, menu)

            val actionFavoriteItem = menu?.findItem(R.id.action_favorite)

            //お気に入り状態に応じて、アイコンを更新する。
            updateFavoriteItem(actionFavoriteItem)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun updateFavoriteItem(actionFavoriteItem: MenuItem?) {
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        // user_id
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

            dataBaseReference.child(FavoritesPATH).child(userId).child(mQuestion.questionUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as? Map<*, *> ?: ""
                    if(map == ""){
                        actionFavoriteItem?.setIcon(R.drawable.ic_star_border)
                    }else{
                        actionFavoriteItem?.setIcon(R.drawable.ic_star)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    actionFavoriteItem?.setIcon(R.drawable.ic_star_border)
                }
            })

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_favorite -> {
            val dataBaseReference = FirebaseDatabase.getInstance().reference

            // user_id
            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            dataBaseReference.child(FavoritesPATH).child(userId).child(mQuestion.questionUid)
                .addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val map = snapshot.value as? Map<*, *> ?: ""
                            if (map == "") {
                                addFavorite(item)
                            } else {
                                deleteFavorite(item)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(baseContext,"失敗", Toast.LENGTH_SHORT).show()
                        }
                    })

            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun addFavorite(item: MenuItem) {

        // user_id
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        val mFavoriteRef = dataBaseReference.child(FavoritesPATH).child(userId).child((mQuestion.questionUid))

        val data = HashMap<String, Any>()
        val answerData = HashMap<String, ArrayList<Answer>>()

        data["uid"] = mQuestion.uid
        data["title"] = mQuestion.title
        data["body"] = mQuestion.body
        data["name"] = mQuestion.name

        if (mQuestion.imageBytes.isNotEmpty()){
            val imageBytes = mQuestion.imageBytes
            val bitmapString = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            data["image"] = bitmapString
        }

        if (mQuestion.answers.isNotEmpty()){
            data["answers"] = mQuestion.answers
        }
        data["genre"] = mQuestion.genre

        mFavoriteRef.setValue(data)

        updateFavoriteItem(item)

//        val answerData = HashMap<String, String>()
//
//        // UID
//        answerData["uid"] = FirebaseAuth.getInstance().currentUser!!.uid
//
//        // 表示名
//        // Preferenceから名前を取る
//        val sp = PreferenceManager.getDefaultSharedPreferences(this)
//        val name = sp.getString(NameKEY, "")
//        answerData["name"] = name!!
//
//        // 回答を取得する
//        val answer = answerEditText.text.toString()
//
//        if (answer.isEmpty()) {
//            // 回答が入力されていない時はエラーを表示するだけ
//            Snackbar.make(v, getString(R.string.answer_error_message), Snackbar.LENGTH_LONG).show()
//            return
//        }
//        answerData["body"] = answer
//
//        progressBar.visibility = View.VISIBLE
//        answerRef.push().setValue(data, this)


    }

    private fun deleteFavorite(item: MenuItem) {

        // user_id
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        val mFavoriteRef = dataBaseReference.child(FavoritesPATH).child(userId).child((mQuestion.questionUid))

        mFavoriteRef.removeValue()

        updateFavoriteItem(item)
    }

}