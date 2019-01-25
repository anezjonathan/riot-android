/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.fragments.keybackupsetup

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import im.vector.Matrix
import im.vector.R
import im.vector.activity.KeybackupSetupActivity
import im.vector.activity.MXCActionBarActivity
import im.vector.activity.VectorAppCompatActivity
import im.vector.fragments.VectorBaseFragment
import java.lang.Exception

class KeybackupSetupStep3Fragment : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.keybackup_setup_step3_fragment

    @BindView(R.id.keybackupsetup_step3_copy_button)
    lateinit var mCopyButton: Button

    @BindView(R.id.keybackupsetup_step3_button)
    lateinit var mFinishButton: Button

    @BindView(R.id.keybackup_recovery_key_text)
    lateinit var mRecoveryKeyTextView: TextView

    @BindView(R.id.keybackup_recovery_key_spinner)
    lateinit var mSpinner: ProgressBar

    @BindView(R.id.keybackup_recovery_key_spinner_text)
    lateinit var mSpinnerStatusText: TextView

    @BindView(R.id.keybackupsetup_step3_root)
    lateinit var mRootLayout: ViewGroup

    companion object {
        fun newInstance() = KeybackupSetupStep3Fragment()
    }

    private lateinit var viewModel: KeybackupSetupSharedViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(KeybackupSetupSharedViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        viewModel.prepareRecoverFailError.observe(this, Observer { error ->
            if (error != null) {
                activity?.run {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.unknown_error)
                            .setMessage(error.localizedMessage)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                //nop
                                viewModel.prepareRecoverFailError.value = null
                                activity?.onBackPressed()
                            }
                            .show()
                }
            }
        })

        val session = (activity as? MXCActionBarActivity)?.session
                ?: Matrix.getInstance(context)?.getSession(null)

        if (viewModel.recoveryKey.value == null) {
            viewModel.prepareRecoveryKey(session)
        }

        viewModel.recoveryKey.observe(this, Observer { newValue ->
            TransitionManager.beginDelayedTransition(mRootLayout)
            if (newValue == null || newValue.isEmpty()) {
                mSpinner.visibility = View.VISIBLE
                mSpinnerStatusText.visibility = View.VISIBLE
                mSpinner.animate()
                mRecoveryKeyTextView.text = null
                mRecoveryKeyTextView.visibility = View.GONE
                mCopyButton.visibility = View.GONE
                mFinishButton.visibility = View.GONE
            } else {
                mSpinner.visibility = View.GONE
                mSpinnerStatusText.visibility = View.GONE

                mRecoveryKeyTextView.text = newValue.replace(" ", "").chunked(16).map {
                    it.chunked(4).joinToString(" ")
                }.joinToString("\n")

                mRecoveryKeyTextView.visibility = View.VISIBLE
                mCopyButton.visibility = View.VISIBLE
                mFinishButton.visibility = View.VISIBLE
            }
        })

        mCopyButton.setOnClickListener {
            val recoveryKey = viewModel.recoveryKey.value
            if (recoveryKey != null) {
                val share = Intent(android.content.Intent.ACTION_SEND)
                share.type = "text/plain"
                share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                // Add data to the intent, the receiving app will decide
                // what to do with it.
                share.putExtra(Intent.EXTRA_SUBJECT, context?.getString(R.string.recovery_key))
                share.putExtra(Intent.EXTRA_TEXT, recoveryKey)

                startActivity(Intent.createChooser(share, context?.getString(R.string.keybackup_setup_step3_share_intent_chooser_title)))
                viewModel.copyHasBeenMade = true
            }
        }

        viewModel.creatingBackupError.observe(this, Observer { error ->
            if (error != null) {
                activity?.let {
                    AlertDialog.Builder(it)
                            .setTitle(R.string.unexpected_error)
                            .setMessage(error.localizedMessage)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                //nop
                                viewModel.creatingBackupError.value = null
                                activity?.onBackPressed()
                            }
                            .show()
                }
            }
        })

        viewModel.keysVersion.observe(this, Observer { keysVersion ->
            if (keysVersion != null) {
                activity?.run {
                    val resultIntent = Intent()
                    resultIntent.putExtra(KeybackupSetupActivity.KEY_RESULT, keysVersion.version)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        })

        viewModel.isCreatingBackupVersion.observe(this, Observer { newValue ->
            val isLoading = newValue ?: false
            if (isLoading) {
                (activity as? VectorAppCompatActivity)?.showWaitingView()
            } else {
                (activity as? VectorAppCompatActivity)?.hideWaitingView()
            }
        })


        mFinishButton.setOnClickListener {
            if (viewModel.megolmBackupCreationInfo == null) {
                //nothing
            } else if (viewModel.copyHasBeenMade) {
                val session = (activity as? MXCActionBarActivity)?.session
                        ?: Matrix.getInstance(context)?.getSession(null)
                val keysBackup = session?.crypto?.keysBackup
                if (keysBackup != null) {
                    viewModel.createKeyBackup(keysBackup)
                }
            } else {
                Toast.makeText(context, R.string.keybackup_setup_step3_please_make_copy, Toast.LENGTH_LONG).show()
            }
        }

    }
}
