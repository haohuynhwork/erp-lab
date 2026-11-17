import type { ECharts, EChartsOption } from "echarts";
import { useEffect, useRef } from "react";

/**
 * Minimal ECharts wrapper: loads echarts on first use (keeps it out of the main bundle), resizes with its
 * container, re-renders when `option` changes.
 */
export function EChart({ option, className }: { option: EChartsOption; className?: string }) {
  const ref = useRef<HTMLDivElement>(null);
  const chart = useRef<ECharts | null>(null);
  const latest = useRef(option);
  latest.current = option;

  useEffect(() => {
    let disposed = false;
    let observer: ResizeObserver | undefined;
    import("echarts").then((echarts) => {
      if (disposed || !ref.current) return;
      chart.current = echarts.init(ref.current);
      chart.current.setOption(latest.current, true);
      observer = new ResizeObserver(() => chart.current?.resize());
      observer.observe(ref.current);
    });
    return () => {
      disposed = true;
      observer?.disconnect();
      chart.current?.dispose();
      chart.current = null;
    };
  }, []);

  useEffect(() => {
    chart.current?.setOption(option, true);
  }, [option]);

  return <div ref={ref} className={className ?? "h-72 w-full"} />;
}
