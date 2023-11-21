package org.mewx.wenku8.fragment

import android.os.Bundle

/**
 * This fragment is the parent fragment to hold all specific fragment.
 * All specific fragment is in PagerAdapter.
 */
class RKListFragment : Fragment() {
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?,
                     savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rklist, container, false)
    }

    @Override
    fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Initialize the ViewPager and set an adapter
        val pager: ViewPager = getActivity().findViewById(R.id.rklist_pager)
        pager.setAdapter(MyPagerAdapter(getChildFragmentManager()))

        // Bind the tabs to the ViewPager
        val tabs: PagerSlidingTabStrip = getActivity().findViewById(R.id.rklist_tabs)
        tabs.setViewPager(pager)

        // set page margin
        val pageMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics()) as Int
        pager.setPageMargin(pageMargin)

        // set adapter
        val adapter: MyPagerAdapter = MyPagerAdapter(getChildFragmentManager())
        pager.setAdapter(adapter)
    }

    @Override
    fun onDetach() {
        super.onDetach()
    }

    inner class MyPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm) {
        private val TITLELIST: Array<Wenku8API.NOVELSORTBY?>? = arrayOf<Wenku8API.NOVELSORTBY?>(
                Wenku8API.NOVELSORTBY.allVisit,
                Wenku8API.NOVELSORTBY.allVote,
                Wenku8API.NOVELSORTBY.monthVisit,
                Wenku8API.NOVELSORTBY.monthVote,
                Wenku8API.NOVELSORTBY.weekVisit,
                Wenku8API.NOVELSORTBY.weekVote,
                Wenku8API.NOVELSORTBY.dayVisit,
                Wenku8API.NOVELSORTBY.dayVote,
                Wenku8API.NOVELSORTBY.postDate,
                Wenku8API.NOVELSORTBY.goodNum,
                Wenku8API.NOVELSORTBY.size,
                Wenku8API.NOVELSORTBY.fullFlag)

        @Override
        fun getPageTitle(position: Int): CharSequence? {
            return getResources().getString(Wenku8API.getNOVELSORTBY_ChsId(TITLELIST.get(position)))
        }

        @Override
        fun getCount(): Int {
            return TITLELIST.size
        }

        @Override
        fun getItem(type: Int): Fragment? {
            val bundle = Bundle()
            bundle.putString("type", Wenku8API.getNOVELSORTBY(TITLELIST.get(type)))
            return NovelItemListFragment.newInstance(bundle)
        }
    }
}