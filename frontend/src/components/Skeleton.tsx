import type { CSSProperties } from 'react';

type SkeletonProps = {
  className?: string;
  style?: CSSProperties;
};

export function Skeleton({ className = '', style }: SkeletonProps) {
  return <div className={`skeleton ${className}`.trim()} style={style} aria-hidden />;
}

export function QuizGridSkeleton() {
  return (
    <div className="quiz-grid" aria-busy="true" aria-label="Đang tải danh sách quiz">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="card quiz-card-skeleton">
          <Skeleton style={{ height: '1.1rem', width: '70%', marginBottom: '0.65rem' }} />
          <Skeleton style={{ height: '0.85rem', width: '45%' }} />
          <Skeleton style={{ height: '0.85rem', width: '90%', marginTop: '0.75rem' }} />
        </div>
      ))}
    </div>
  );
}
