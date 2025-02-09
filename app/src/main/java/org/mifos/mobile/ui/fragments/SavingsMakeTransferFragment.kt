package org.mifos.mobile.ui.fragments

import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener

import androidx.appcompat.widget.AppCompatButton

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick

import com.github.therajanmaurya.sweeterror.SweetUIErrorHandler
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

import org.mifos.mobile.R
import org.mifos.mobile.models.payload.AccountDetail
import org.mifos.mobile.models.payload.TransferPayload
import org.mifos.mobile.models.templates.account.AccountOption
import org.mifos.mobile.models.templates.account.AccountOptionsTemplate
import org.mifos.mobile.presenters.SavingsMakeTransferPresenter
import org.mifos.mobile.ui.activities.base.BaseActivity
import org.mifos.mobile.ui.adapters.AccountsSpinnerAdapter
import org.mifos.mobile.ui.enums.TransferType
import org.mifos.mobile.ui.fragments.base.BaseFragment
import org.mifos.mobile.ui.views.SavingsMakeTransferMvpView
import org.mifos.mobile.utils.*

import java.util.*
import javax.inject.Inject

/**
 * Created by Rajan Maurya on 10/03/17.
 */
class SavingsMakeTransferFragment : BaseFragment(), SavingsMakeTransferMvpView {

    @kotlin.jvm.JvmField
    @BindView(R.id.pay_to_field)
    var payToField: MaterialAutoCompleteTextView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.pay_from_field)
    var payFromField: MaterialAutoCompleteTextView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.amount_field)
    var amountField: TextInputEditText? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.pay_to_field_wrapper)
    var payToFieldWrapper: TextInputLayout? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.pay_from_field_wrapper)
    var payFromFieldWrapper: TextInputLayout? = null


    @kotlin.jvm.JvmField
    @BindView(R.id.amount_field_wrapper)
    var amountFieldWrapper: TextInputLayout? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.process_one)
    var pvOne: ProcessView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.process_two)
    var pvTwo: ProcessView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.process_three)
    var pvThree: ProcessView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.process_four)
    var pvFour: ProcessView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.btn_pay_to)
    var btnPayTo: AppCompatButton? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.btn_pay_from)
    var btnPayFrom: AppCompatButton? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.btn_amount)
    var btnAmount: AppCompatButton? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.ll_review)
    var llReview: LinearLayout? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.tv_enter_remark)
    var tvEnterRemark: TextView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.et_remark)
    var etRemark: EditText? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.ll_make_transfer)
    var layoutMakeTransfer: LinearLayout? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.layout_error)
    var layoutError: View? = null

    @kotlin.jvm.JvmField
    @Inject
    var savingsMakeTransferPresenter: SavingsMakeTransferPresenter? = null
    var rootView: View? = null
    private var transferPayload: TransferPayload? = null
    private var transferDate: String? = null
    private var toAccountOption: AccountOption? = null
    private var fromAccountOption: AccountOption? = null
    private var accountOptionsTemplate: AccountOptionsTemplate? = null
    private var transferType: String? = null
    private var payTo: String? = null
    private var payFrom: String? = null
    private var accountId: Long? = 0
    private var outStandingBalance: Double? = 0.0
    private var isLoanRepayment = false
    private var sweetUIErrorHandler: SweetUIErrorHandler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            accountId = arguments?.getLong(Constants.ACCOUNT_ID)
            transferType = arguments?.getString(Constants.TRANSFER_TYPE)
            if (arguments?.getBoolean(Constants.LOAN_REPAYMENT, false) == true) {
                isLoanRepayment = true
                outStandingBalance = arguments?.getDouble(Constants.OUTSTANDING_BALANCE)
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_savings_make_transfer, container, false)
        (activity as BaseActivity?)?.activityComponent?.inject(this)
        setToolbarTitle(getString(R.string.transfer))
        ButterKnife.bind(this, rootView!!)
        savingsMakeTransferPresenter?.attachView(this)
        sweetUIErrorHandler = SweetUIErrorHandler(activity, rootView)
        showUserInterface()
        if (savedInstanceState == null) {
            savingsMakeTransferPresenter?.loanAccountTransferTemplate()
        }
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Constants.TEMPLATE, accountOptionsTemplate)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            showSavingsAccountTemplate(savedInstanceState.getParcelable<Parcelable>(Constants.TEMPLATE) as AccountOptionsTemplate)
        }
    }

    /**
     * Checks validation of `etRemark` and then opens [TransferProcessFragment] for
     * initiating the transfer
     */
    @OnClick(R.id.btn_review_transfer)
    fun reviewTransfer() {
        if (etRemark?.text.toString().trim { it <= ' ' } == "") {
            showToaster(getString(R.string.remark_is_mandatory))
            return
        }
        transferPayload = TransferPayload()
        transferPayload?.fromAccountId = fromAccountOption?.accountId
        transferPayload?.fromClientId = fromAccountOption?.clientId
        transferPayload?.fromAccountType = fromAccountOption?.accountType?.id
        transferPayload?.fromOfficeId = fromAccountOption?.officeId
        transferPayload?.toOfficeId = toAccountOption?.officeId
        transferPayload?.toAccountId = toAccountOption?.accountId
        transferPayload?.toClientId = toAccountOption?.clientId
        transferPayload?.toAccountType = toAccountOption?.accountType?.id
        transferPayload?.transferDate = transferDate
        transferPayload?.transferAmount = amountField?.text.toString().toDouble()
        transferPayload?.transferDescription = etRemark?.text.toString()
        transferPayload?.fromAccountNumber = fromAccountOption?.accountNo
        transferPayload?.toAccountNumber = toAccountOption?.accountNo
        (activity as BaseActivity?)?.replaceFragment(TransferProcessFragment.newInstance(transferPayload, TransferType.SELF), true, R.id.container)
    }

    /**
     * Cancels the transfer by poping current Fragment
     */
    @OnClick(R.id.btn_cancel_transfer)
    fun cancelTransfer() {
        activity?.supportFragmentManager?.popBackStack()
    }

    @OnClick(R.id.btn_try_again)
    fun onRetry() {
        if (Network.isConnected(context)) {
            sweetUIErrorHandler?.hideSweetErrorLayoutUI(layoutMakeTransfer, layoutError)
            savingsMakeTransferPresenter?.loanAccountTransferTemplate()
        } else {
            Toaster.show(rootView, getString(R.string.internet_not_connected))
        }
    }

    /**
     * Setting up basic components
     */
    override fun showUserInterface() {
        pvOne?.setCurrentActive()
        payFromField?.setOnItemClickListener { parent, view, position, id ->
            toAccountOption = accountOptionsTemplate?.toAccountOptions?.get(position)
            payTo = toAccountOption?.accountNo
            println("paytofield item selected payTo:$payTo")
            updateDetails()
        }
        payToField?.setOnItemClickListener { parent, view, position, id ->
            fromAccountOption = accountOptionsTemplate?.fromAccountOptions?.get(position)
            payFrom = fromAccountOption?.accountNo
            println("payfrom item selected payFrom:$payFrom")
            updateDetails()
        }
        transferDate = DateHelper.getSpecificFormat(DateHelper.FORMAT_dd_MMMM_yyyy,
                getTodayFormatted())
        if (isLoanRepayment) {
            amountField?.setText(outStandingBalance.toString())
            amountFieldWrapper?.isFocusable = false
        }
    }

    fun updateDetails(){
        when (transferType) {
            Constants.TRANSFER_PAY_TO -> {
                setToolbarTitle(getString(R.string.deposit))
                toAccountOption = savingsMakeTransferPresenter
                    ?.searchAccount(accountOptionsTemplate?.toAccountOptions, accountId)
                payToFieldWrapper?.isEnabled = false
                pvOne?.setCurrentCompleted()
            }
            Constants.TRANSFER_PAY_FROM -> {
                setToolbarTitle(getString(R.string.transfer))
                fromAccountOption = savingsMakeTransferPresenter
                    ?.searchAccount(accountOptionsTemplate?.fromAccountOptions, accountId)
                payFromFieldWrapper?.isEnabled = false
                payFromFieldWrapper?.visibility = View.VISIBLE
                pvTwo?.setCurrentCompleted()
            }
        }
    }

    /**
     * Provides with `accountOptionsTemplate` fetched from server which is used to update
     * `listPayFrom` and `listPayTo`
     *
     * @param accountOptionsTemplate Template for account transfer
     */
    override fun showSavingsAccountTemplate(accountOptionsTemplate: AccountOptionsTemplate?) {
        this.accountOptionsTemplate = accountOptionsTemplate
        payToField?.setAdapter(AccountsSpinnerAdapter(requireContext(),
            savingsMakeTransferPresenter?.getAccountNumbers(accountOptionsTemplate?.toAccountOptions, false)!!
        ))
        payFromField?.setAdapter(AccountsSpinnerAdapter(requireContext(),
            savingsMakeTransferPresenter?.getAccountNumbers(accountOptionsTemplate?.toAccountOptions, true)!!
        ))
    }

    /**
     * Shows a {@link Snackbar} with `message`
     *
     * @param message String to be shown
     */
    override fun showToaster(message: String?) {
        Toaster.show(rootView, message)
    }

    /**
     * It is called whenever any error occurs while executing a request
     *
     * @param message Error message that tells the user about the problem.
     */
    override fun showError(message: String?) {
        if (!Network.isConnected(context)) {
            sweetUIErrorHandler?.showSweetNoInternetUI(layoutMakeTransfer, layoutError)
        } else {
            sweetUIErrorHandler?.showSweetErrorUI(message, layoutMakeTransfer, layoutError)
            Toaster.show(rootView, message)
        }
    }

    override fun showProgressDialog() {
        showMifosProgressDialog(getString(R.string.making_transfer))
    }

    override fun hideProgressDialog() {
        hideMifosProgressDialog()
    }

    override fun showProgress() {
        layoutMakeTransfer?.visibility = View.GONE
        showProgressBar()
    }

    override fun hideProgress() {
        layoutMakeTransfer?.visibility = View.VISIBLE
        hideProgressBar()
    }

    /**
     * Callback for `spPayFrom` and `spPayTo`
     */

    /**
     * Disables `spPayTo` [Spinner] and sets `pvOne` to completed and make
     * `pvTwo` active
     */
    @OnClick(R.id.btn_pay_to)
    fun payToSelected() {
        pvOne?.setCurrentCompleted()
        pvTwo?.setCurrentActive()
        btnPayTo?.visibility = View.GONE
        btnPayFrom?.visibility = View.VISIBLE
        payFromFieldWrapper?.visibility = View.VISIBLE
        payToFieldWrapper?.isEnabled = false
    }

    /**
     * Checks validation of `spPayTo` [Spinner].<br></br>
     * Disables `spPayFrom` [Spinner] and sets `pvTwo` to completed and make
     * `pvThree` active
     */
    @OnClick(R.id.btn_pay_from)
    fun payFromSelected() {
        if (payTo == payFrom) {
            showToaster(getString(R.string.error_same_account_transfer))
            return
        }
        pvTwo?.setCurrentCompleted()
        pvThree?.setCurrentActive()
        btnPayFrom?.visibility = View.GONE
        amountFieldWrapper?.visibility = View.VISIBLE
        btnAmount?.visibility = View.VISIBLE
        payFromFieldWrapper?.isEnabled = false
    }

    /**
     * Checks validation of `etAmount` [EditText].<br></br>
     * Disables `etAmount` and sets `pvThree` to completed and make
     * `pvFour` active
     */
    @OnClick(R.id.btn_amount)
    fun amountSet() {
        if (amountField?.text.toString() == "") {
            showToaster(getString(R.string.enter_amount))
            return
        }
        if (amountField?.text.toString() == ".") {
            showToaster(getString(R.string.invalid_amount))
            return
        }
        if (amountField?.text.toString().toDouble() == 0.0) {
            showToaster(getString(R.string.amount_greater_than_zero))
            return
        }
        pvThree?.setCurrentCompleted()
        pvFour?.setCurrentActive()
        btnAmount?.visibility = View.GONE
        tvEnterRemark?.visibility = View.GONE
        etRemark?.visibility = View.VISIBLE
        llReview?.visibility = View.VISIBLE
        amountFieldWrapper?.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_transfer, menu)
        Utils.setToolbarIconColor(activity, menu, R.color.white)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item?.itemId == R.id.menu_refresh_transfer) {
            val transaction = fragmentManager?.beginTransaction()
            val currFragment = activity?.supportFragmentManager
                    ?.findFragmentById(R.id.container)
            if (currFragment != null) {
                transaction?.detach(currFragment)
                transaction?.attach(currFragment)
            }
            transaction?.commit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideProgress()
        hideMifosProgressDialog()
        savingsMakeTransferPresenter?.detachView()
    }

    companion object {
        /**
         * Provides an instance of [SavingsMakeTransferFragment], use `transferType` as
         * `Constants.TRANSFER_PAY_TO` when we want to deposit and
         * `Constants.TRANSFER_PAY_FROM` when we want to make a transfer
         *
         * @param accountId    Saving account Id
         * @param transferType Type of transfer i.e. `Constants.TRANSFER_PAY_TO` or
         * `Constants.TRANSFER_PAY_FROM`
         * @return Instance of [SavingsMakeTransferFragment]
         */
        fun newInstance(accountId: Long?, transferType: String?): SavingsMakeTransferFragment {
            val transferFragment = SavingsMakeTransferFragment()
            val args = Bundle()
            if (accountId != null) args.putLong(Constants.ACCOUNT_ID, accountId)
            args.putString(Constants.TRANSFER_TYPE, transferType)
            transferFragment.arguments = args
            return transferFragment
        }

        fun newInstance(
                accountId: Long?, outstandingBalance: Double?,
                transferType: String?
        ): SavingsMakeTransferFragment {
            val transferFragment = SavingsMakeTransferFragment()
            val args = Bundle()
            if (accountId != null) args.putLong(Constants.ACCOUNT_ID, accountId)
            args.putString(Constants.TRANSFER_TYPE, transferType)
            if (outstandingBalance != null) args.putDouble(Constants.OUTSTANDING_BALANCE, outstandingBalance)
            args.putBoolean(Constants.LOAN_REPAYMENT, true)
            transferFragment.arguments = args
            return transferFragment
        }
    }
}