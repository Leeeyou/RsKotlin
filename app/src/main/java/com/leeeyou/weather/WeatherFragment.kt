package com.leeeyou.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.leeeyou.BaseFragment
import com.leeeyou.R
import com.leeeyou.util.inflate

/**
 * ClassName: WeatherFragment
 * Description: 天气展示
 *
 * Author:      leeeyou
 * Date:        2017/4/24 13:46
 */
class WeatherFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.fragment_weather)
    }

}